/*
 * Copyright 2017 Pavel Semak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alphamovie.lib;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoRenderer implements GLTextureView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final int COLOR_MAX_VALUE = 255;
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static String TAG = "VideoRender";
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private final float[] triangleVerticesData = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.f, 0.f,
        1.0f, -1.0f, 0, 1.f, 0.f,
        -1.0f, 1.0f, 0, 0.f, 1.f,
        1.0f, 1.0f, 0, 1.f, 1.f,
    };

    private FloatBuffer triangleVertices;

    private int uMVPMatrixHandle;
    private int uSTMatrixHandle;
    private float[] mVPMatrix = new float[16];
    private float[] sTMatrix = new float[16];
    private int aPositionHandle;
    private int aTextureHandle;
    private int vertexShaderID;
    private int fragmentShaderID;
    private int programID;
    private int textureID;

    private SurfaceTexture surface;
    private boolean updateSurface = false;
    private boolean updateShaderProgram = false;
    private OnSurfacePrepareListener onSurfacePrepareListener;
    private Shader shader;

    VideoRenderer() {
        triangleVertices =
            ByteBuffer
                .allocateDirect(triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);

        Matrix.setIdentityM(sTMatrix, 0);

        shader = new PassthroughShader();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        Shader shader = this.shader;

        synchronized (this) {
            if (updateShaderProgram) {
                updateShaderProgram();
                updateShaderProgram = false;
            }

            if (updateSurface) {
                surface.updateTexImage();
                surface.getTransformMatrix(sTMatrix);
                updateSurface = false;
            }
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        GLES20.glUseProgram(programID);
        GLUtility.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureID);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            triangleVertices
        );
        GLUtility.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLUtility.checkGlError("glEnableVertexAttribArray aPositionHandle");

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(
            aTextureHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            triangleVertices
        );
        GLUtility.checkGlError("glVertexAttribPointer aTextureHandle");
        GLES20.glEnableVertexAttribArray(aTextureHandle);
        GLUtility.checkGlError("glEnableVertexAttribArray aTextureHandle");

        Matrix.setIdentityM(mVPMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mVPMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, sTMatrix, 0);

        shader.setUniforms();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLUtility.checkGlError("glDrawArrays");

        GLES20.glFinish();
    }

    @Override
    public void onSurfaceDestroyed(GL10 gl) {
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        updateShaderProgram();
        if (programID == 0)
            return;

        prepareSurface();
    }

    private void updateShaderProgram() {
        if (programID != 0) {
            GLES20.glDeleteProgram(programID);
            programID = 0;
        }
        programID = createProgram(shader.getVertexShader(), shader.getFragmentShader());
        if (programID == 0)
            throw new RuntimeException("Failed to create program");

        updateAttributesAndUniforms();
    }

    private void updateAttributesAndUniforms() {
        aPositionHandle = GLES20.glGetAttribLocation(programID, "aPosition");
        GLUtility.checkGlError("glGetAttribLocation aPosition");
        if (aPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        aTextureHandle = GLES20.glGetAttribLocation(programID, "aTextureCoord");
        GLUtility.checkGlError("glGetAttribLocation aTextureCoord");
        if (aTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programID, "uMVPMatrix");
        GLUtility.checkGlError("glGetUniformLocation uMVPMatrix");
        if (uMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        uSTMatrixHandle = GLES20.glGetUniformLocation(programID, "uSTMatrix");
        GLUtility.checkGlError("glGetUniformLocation uSTMatrix");
        if (uSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        shader.getUniformLocations(programID);
    }

    private void prepareSurface() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureID);
        GLUtility.checkGlError("glBindTexture textureID");

        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        );
        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        );

        surface = new SurfaceTexture(textureID);
        surface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(this.surface);
        onSurfacePrepareListener.surfacePrepared(surface);

        synchronized (this) {
            updateSurface = false;
        }
    }

    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        if (vertexShaderID != 0) {
            GLES20.glDeleteShader(vertexShaderID);
            vertexShaderID = 0;
        }

        if (fragmentShaderID != 0) {
            GLES20.glDeleteShader(fragmentShaderID);
            fragmentShaderID = 0;
        }

        int vertexShaderIDTemp = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShaderIDTemp == 0) {
            return 0;
        }
        int fragmentShaderIDTemp = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShaderIDTemp == 0) {
            return 0;
        }

        vertexShaderID = vertexShaderIDTemp;
        fragmentShaderID = fragmentShaderIDTemp;

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShaderIDTemp);
            GLUtility.checkGlError("glAttachShader");
            GLES20.glAttachShader(program, fragmentShaderIDTemp);
            GLUtility.checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    void setOnSurfacePrepareListener(OnSurfacePrepareListener onSurfacePrepareListener) {
        this.onSurfacePrepareListener = onSurfacePrepareListener;
    }

    public void setShader(Shader shader) {
        synchronized (this) {
            this.shader = shader;
            updateShaderProgram = true;
        }
    }

    interface OnSurfacePrepareListener {
        void surfacePrepared(Surface surface);
    }
}