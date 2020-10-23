package com.alphamovie.lib;

public class InvertColorsShader extends ShaderBase {
    @Override
    public String getFragmentShader() {
        return
            "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "void main() {\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  color.rgb = 1.0 - color.rgb;\n"
                + "  gl_FragColor = color;\n"
                + "}\n";
    }
}
