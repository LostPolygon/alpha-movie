package com.alphamovie.lib;

import android.graphics.Color;
import android.opengl.GLES20;

public class AdvancedChromaKeyShader extends ShaderBase {
    private final float[] uMaskYCrCbCache = new float[3];

    private int mChromaKeyColor;
    private float mThresholdSensitivity;
    private float mSmoothing;

    private volatile int uThresholdSensitivityHandle;
    private volatile int uSmoothingHandle;
    private volatile int uMaskYCrCbHandle;

    public int getChromaKeyColor() {
        return mChromaKeyColor;
    }

    public void setChromaKeyColor(int chromaKeyColor) {
        mChromaKeyColor = chromaKeyColor;
    }

    public float getThresholdSensitivity() {
        return mThresholdSensitivity;
    }

    public void setThresholdSensitivity(float thresholdSensitivity) {
        mThresholdSensitivity = Math.max(0, Math.min(1, thresholdSensitivity));
    }

    public float getSmoothing() {
        return mSmoothing;
    }

    public void setSmoothing(float smoothing) {
        mSmoothing = Math.max(0, Math.min(1, smoothing));
    }

    @Override
    public String getFragmentShader() {
        return
            "#extension GL_OES_EGL_image_external : require\n\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "uniform mediump float uThresholdSensitivity;\n" +
                "uniform mediump float uSmoothing;\n" +
                "uniform vec3 uMaskYCrCb;\n" +
                "\n" +
                "void main() {\n" +
                "  vec4 textureColor = texture2D(sTexture, vTextureCoord);" +
                "  \n" +
                "  float Y = 0.2989 * textureColor.r + 0.5866 * textureColor.g + 0.1145 * textureColor.b;\n" +
                "  float Cr = 0.7132 * (textureColor.r - Y);\n" +
                "  float Cb = 0.5647 * (textureColor.b - Y);\n" +
                "  \n" +
                "  float blendValue = smoothstep(uThresholdSensitivity, uThresholdSensitivity + uSmoothing, distance(vec2(Cr, Cb), vec2(uMaskYCrCb.r, uMaskYCrCb.b)));\n" +
                "  gl_FragColor = vec4(textureColor.rgb, textureColor.a * blendValue);\n" +
                "}";
    }

    @Override
    public void getUniformLocations(int programID) {
        uThresholdSensitivityHandle = getUniformLocation(programID, "uThresholdSensitivity");
        uSmoothingHandle = getUniformLocation(programID, "uSmoothing");
        uMaskYCrCbHandle = getUniformLocation(programID, "uMaskYCrCb");
    }

    @Override
    public void setUniforms() {
        final float chromaKeyR = Color.red(mChromaKeyColor) / 255f;
        final float chromaKeyG = Color.green(mChromaKeyColor) / 255f;
        final float chromaKeyB = Color.blue(mChromaKeyColor) / 255f;

        float chromaKeyY = 0.2989f * chromaKeyR + 0.5866f * chromaKeyG + 0.1145f * chromaKeyB;
        float chromaKeyCr = 0.7132f * (chromaKeyR - chromaKeyY);
        float chromaKeyCb = 0.5647f * (chromaKeyB - chromaKeyY);

        uMaskYCrCbCache[0] = chromaKeyY;
        uMaskYCrCbCache[1] = chromaKeyCr;
        uMaskYCrCbCache[2] = chromaKeyCb;

        GLES20.glUniform1f(uThresholdSensitivityHandle, mThresholdSensitivity);
        GLUtility.checkGlError("glUniform1f uThresholdSensitivityHandle");
        GLES20.glUniform1f(uSmoothingHandle, mSmoothing);
        GLUtility.checkGlError("glUniform1f uSmoothing");
        GLES20.glUniform3fv(uMaskYCrCbHandle, 1, uMaskYCrCbCache, 0);
        GLUtility.checkGlError("glUniform3fv uMaskYCrCb");
    }

    private static int getUniformLocation(int programID, String name) {
        int handle = GLES20.glGetUniformLocation(programID, name);
        GLUtility.checkGlError("glGetUniformLocation " + name);
        if (handle == -1)
            throw new RuntimeException("Could not get uniform location for " + name);

        return handle;
    }
}
