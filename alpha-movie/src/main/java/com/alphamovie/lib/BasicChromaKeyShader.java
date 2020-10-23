package com.alphamovie.lib;

public class BasicChromaKeyShader extends ShaderBase {
    @Override
    public String getFragmentShader() {
        return
            "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 vTextureCoord;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "varying mediump float text_alpha_out;\n"
                + "void main() {\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  float red = %f;\n"
                + "  float green = %f;\n"
                + "  float blue = %f;\n"
                + "  float accuracy = %f;\n"
                + "  if (abs(color.r - red) <= accuracy && abs(color.g - green) <= accuracy && abs(color.b - blue) <= accuracy) {\n"
                + "      gl_FragColor = vec4(color.r, color.g, color.b, 0.0);\n"
                + "  } else {\n"
                + "      gl_FragColor = vec4(color.r, color.g, color.b, 1.0);\n"
                + "  }\n"
                + "}\n";
    }
}
