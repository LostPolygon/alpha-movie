package com.alphamovie.lib;

public abstract class ShaderBase implements Shader {


    public ShaderBase() {

    }

    @Override
    public String getVertexShader() {
        return
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";
    }

    @Override
    public abstract String getFragmentShader();

    @Override
    public void getUniformLocations(int programID) {
    }

    @Override
    public void setUniforms() {
    }
}
