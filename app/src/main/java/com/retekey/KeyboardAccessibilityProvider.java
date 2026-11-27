package com.retekey;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import java.util.List;

/**
 * Exposes each drawn key cell as a virtual accessibility node (TalkBack / Explore-by-Touch)
 * isolated behind API 16 {@link TargetApi} so the keyboard view remains safe on API 14–15.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
final class KeyboardAccessibilityProvider extends AccessibilityNodeProvider {
    interface HostCallbacks {
        KeyboardLayout currentLayout();

        ModifierCoordinator modifiers();

        void activateTap(int rowIndex, int keyIndex);

        boolean activateLongPress(int rowIndex, int keyIndex);
    }

    private final View host;
    private final HostCallbacks callbacks;
    private final int[] screenLocation = new int[2];
    private int focusedVirtualId = View.NO_ID;
    private int hoveredVirtualId = View.NO_ID;

    private KeyboardAccessibilityProvider(View host, HostCallbacks callbacks) {
        this.host = host;
        this.callbacks = callbacks;
    }

    static Object install(final View host, HostCallbacks callbacks) {
        final KeyboardAccessibilityProvider provider =
            new KeyboardAccessibilityProvider(host, callbacks);
        host.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
                return provider;
            }
        });
        return provider;
    }

    static boolean dispatchHover(Object instance, MotionEvent event) {
        return instance instanceof KeyboardAccessibilityProvider
            && ((KeyboardAccessibilityProvider) instance).onHoverEvent(event);
    }

    private List<KeyboardAccessibilityModel.KeyNode> snapshotNodes() {
        return KeyboardAccessibilityModel.nodes(
            callbacks.currentLayout(),
            host.getWidth(),
            host.getHeight(),
            Build.VERSION.SDK_INT,
            callbacks.modifiers()
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        List<KeyboardAccessibilityModel.KeyNode> nodes = snapshotNodes();
        if (virtualViewId == HOST_VIEW_ID) {
            AccessibilityNodeInfo root = AccessibilityNodeInfo.obtain(host);
            host.onInitializeAccessibilityNodeInfo(root);
            for (KeyboardAccessibilityModel.KeyNode node : nodes) {
                root.addChild(host, node.virtualId);
            }
            return root;
        }
        KeyboardAccessibilityModel.KeyNode node =
            KeyboardAccessibilityModel.findByVirtualId(nodes, virtualViewId);
        if (node == null) {
            return null;
        }
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        info.setPackageName(host.getContext().getPackageName());
        info.setClassName("android.widget.Button");
        info.setSource(host, virtualViewId);
        info.setParent(host);
        info.setContentDescription(node.description);
        info.setEnabled(node.enabled);
        info.setClickable(node.clickable);
        info.setLongClickable(node.longClickable);
        info.setCheckable(node.checkable);
        info.setChecked(node.checked);
        info.setVisibleToUser(host.getVisibility() == View.VISIBLE);
        info.setBoundsInParent(new Rect(node.left, node.top, node.right, node.bottom));
        host.getLocationOnScreen(screenLocation);
        info.setBoundsInScreen(new Rect(
            node.left + screenLocation[0],
            node.top + screenLocation[1],
            node.right + screenLocation[0],
            node.bottom + screenLocation[1]
        ));
        boolean focused = focusedVirtualId == virtualViewId;
        info.setAccessibilityFocused(focused);
        if (focused) {
            info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
        } else {
            info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        }
        if (node.clickable) {
            info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        if (node.longClickable) {
            info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        }
        return info;
    }

    @Override
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        if (virtualViewId == HOST_VIEW_ID) {
            return host.performAccessibilityAction(action, arguments);
        }
        List<KeyboardAccessibilityModel.KeyNode> nodes = snapshotNodes();
        KeyboardAccessibilityModel.KeyNode node =
            KeyboardAccessibilityModel.findByVirtualId(nodes, virtualViewId);
        if (node == null) {
            return false;
        }
        switch (action) {
            case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                if (focusedVirtualId == virtualViewId) {
                    return false;
                }
                focusedVirtualId = virtualViewId;
                sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, node);
                host.invalidate();
                return true;
            case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                if (focusedVirtualId != virtualViewId) {
                    return false;
                }
                focusedVirtualId = View.NO_ID;
                sendEvent(
                    virtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
                    node
                );
                host.invalidate();
                return true;
            case AccessibilityNodeInfo.ACTION_CLICK:
                if (!node.clickable) {
                    return false;
                }
                callbacks.activateTap(node.rowIndex, node.keyIndex);
                sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED, node);
                return true;
            case AccessibilityNodeInfo.ACTION_LONG_CLICK:
                if (!node.longClickable) {
                    return false;
                }
                if (callbacks.activateLongPress(node.rowIndex, node.keyIndex)) {
                    sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, node);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean onHoverEvent(MotionEvent event) {
        AccessibilityManager am = Compat.systemService(
            host.getContext(), Context.ACCESSIBILITY_SERVICE, AccessibilityManager.class);
        if (am == null || !am.isTouchExplorationEnabled()) {
            return false;
        }
        List<KeyboardAccessibilityModel.KeyNode> nodes = snapshotNodes();
        KeyboardAccessibilityModel.KeyNode hit =
            KeyboardAccessibilityModel.hitTest(nodes, event.getX(), event.getY());
        int nextId = hit == null ? View.NO_ID : hit.virtualId;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
                if (nextId != hoveredVirtualId) {
                    int prev = hoveredVirtualId;
                    hoveredVirtualId = nextId;
                    if (nextId != View.NO_ID) {
                        sendEvent(nextId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER, hit);
                    }
                    if (prev != View.NO_ID) {
                        KeyboardAccessibilityModel.KeyNode prevNode =
                            KeyboardAccessibilityModel.findByVirtualId(nodes, prev);
                        sendEvent(prev, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT, prevNode);
                    }
                }
                return nextId != View.NO_ID;
            case MotionEvent.ACTION_HOVER_EXIT:
                if (hoveredVirtualId != View.NO_ID) {
                    int prev = hoveredVirtualId;
                    hoveredVirtualId = View.NO_ID;
                    KeyboardAccessibilityModel.KeyNode prevNode =
                        KeyboardAccessibilityModel.findByVirtualId(nodes, prev);
                    sendEvent(prev, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT, prevNode);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void sendEvent(
            int virtualViewId, int eventType, KeyboardAccessibilityModel.KeyNode node) {
        ViewParent parent = host.getParent();
        if (parent == null) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        event.setPackageName(host.getContext().getPackageName());
        event.setClassName("android.widget.Button");
        event.setEnabled(node == null || node.enabled);
        if (node != null) {
            event.setContentDescription(node.description);
        }
        event.setSource(host, virtualViewId);
        parent.requestSendAccessibilityEvent(host, event);
    }
}
