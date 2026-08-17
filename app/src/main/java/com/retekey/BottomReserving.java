package com.retekey;

/**
 * A view that fills the window and therefore has to be told what is not its to fill.
 *
 * <p>The notepad and the clipboard both measure themselves to the whole screen, which was right
 * until the system's bottom band was reserved under them: the frame around them then came out a
 * band taller than the window, and the keyboard's bottom row was pushed off the screen — visible
 * nowhere, though still pressable where it used to be. They subtract what they are told instead of
 * guessing.
 */
interface BottomReserving {
    void setBottomReserved(int px);
}
