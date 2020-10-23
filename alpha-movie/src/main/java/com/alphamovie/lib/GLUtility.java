package com.alphamovie.lib;

import android.opengl.GLES20;
import android.opengl.GLU;

public final class GLUtility {
    public static void checkGlError(String op) {
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error + " (" + GLU.gluErrorString(error) + ")");
        }
    }
}
