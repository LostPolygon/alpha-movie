package com.alphamovie.lib;

public class PassthroughShader extends ShaderBase {
    @Override
    public String getFragmentShader() {
        return
            "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
                + "}\n";
    }
}
