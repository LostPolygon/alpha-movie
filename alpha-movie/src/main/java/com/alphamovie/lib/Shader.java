package com.alphamovie.lib;

public interface Shader {
    String getVertexShader();
    String getFragmentShader();

    void getUniformLocations(int programID);
    void setUniforms();
}
