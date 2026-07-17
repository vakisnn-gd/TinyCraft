#version 330 core

layout(location = 0) in uvec3 aPackedVertex;

uniform mat4 uViewProjection;
uniform vec3 uChunkOrigin;
uniform vec3 uCameraPosition;
uniform float uTime;
uniform int u_FancyGraphics;
uniform int uRegionMesh;

out vec4 vColor;
out float v_AO;
out float vFogDistance;

vec3 unpackChunkPosition(uint packed) {
    uint xi = packed & 1023u;
    uint yi = (packed >> 10) & 1023u;
    uint zi = (packed >> 20) & 1023u;
    return vec3(xi, yi, zi) * (1.0 / 32.0) - vec3(0.25);
}

vec3 unpackRegionPosition(uint low, uint high) {
    uint xi = low & 8191u;
    uint yi = (low >> 13) & 1023u;
    uint zi = ((low >> 23) & 511u) | ((high & 15u) << 9);
    return vec3(xi, yi, zi) * (1.0 / 32.0) - vec3(0.25);
}

vec4 unpackColor(uint packed) {
    uint alphaByte = packed & 255u;
    bool opaqueFoliageFlag = alphaByte == 252u || alphaByte == 253u;
    bool translucentFoliageFlag = alphaByte == 248u || alphaByte == 249u;
    float alpha = opaqueFoliageFlag ? 1.0 : (translucentFoliageFlag ? 0.92 : float(alphaByte) * (1.0 / 255.0));
    return vec4(
        float((packed >> 24) & 255u),
        float((packed >> 16) & 255u),
        float((packed >> 8) & 255u),
        alpha * 255.0
    ) * (1.0 / 255.0);
}

void main() {
    uint positionAndFlags = aPackedVertex.x;
    uint colorPacked;
    vec3 localPosition;
    if (uRegionMesh != 0) {
        localPosition = unpackRegionPosition(aPackedVertex.x, aPackedVertex.y);
        colorPacked = aPackedVertex.z;
        vColor = unpackColor(colorPacked);
        v_AO = float((aPackedVertex.y >> 4) & 3u);
    } else {
        localPosition = unpackChunkPosition(positionAndFlags);
        colorPacked = aPackedVertex.y;
        vColor = unpackColor(colorPacked);
        v_AO = float((positionAndFlags >> 30) & 3u);
    }
    uint alphaByte = colorPacked & 255u;
    bool foliageVertex = alphaByte == 252u || alphaByte == 253u || alphaByte == 248u || alphaByte == 249u;
    bool upperVertex = alphaByte == 253u || alphaByte == 249u;
    vec3 worldPosition = uCameraPosition + uChunkOrigin + localPosition;
    if (u_FancyGraphics > 0 && foliageVertex && upperVertex) {
        vec3 blockSeed = floor(worldPosition);
        float wave = sin(uTime * 2.0 + blockSeed.x + blockSeed.z) * 0.1;
        localPosition.x += wave;
    }
    bool waterVertex = vColor.a > 0.45 && vColor.a < 0.75;
    if (waterVertex) {
        float wave = sin(worldPosition.x * 0.45 + uTime * 2.1)
            + cos(worldPosition.z * 0.38 + uTime * 1.7);
        localPosition.y += wave * 0.018;
    }
    vec3 viewPosition = uChunkOrigin + localPosition;
    vFogDistance = length(viewPosition);
    gl_Position = uViewProjection * vec4(viewPosition, 1.0);
}
