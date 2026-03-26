package io.github.skubiak0903.bdengine.utils;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import io.github.skubiak0903.bdengine.utils.GivensParameters;

/*
 *  NOTE: Stolen from minecraft implementation: com.mojang.math
 */

public final class GivensParameters {
  private final float sinHalf;
  
  private final float cosHalf;
  
  public GivensParameters(float sinHalf, float cosHalf) {
    this.sinHalf = sinHalf;
    this.cosHalf = cosHalf;
  }
  
  public float sinHalf() {
    return this.sinHalf;
  }
  
  public float cosHalf() {
    return this.cosHalf;
  }
  
  public static GivensParameters fromUnnormalized(float sinHalf, float cosHalf) {
    float w = Math.invsqrt(sinHalf * sinHalf + cosHalf * cosHalf);
    return new GivensParameters(w * sinHalf, w * cosHalf);
  }
  
  public static GivensParameters fromPositiveAngle(float angle) {
    float sin = Math.sin(angle / 2.0F);
    float cos = Math.cosFromSin(sin, angle / 2.0F);
    return new GivensParameters(sin, cos);
  }
  
  public GivensParameters inverse() {
    return new GivensParameters(-this.sinHalf, this.cosHalf);
  }
  
  public Quaternionf aroundX(Quaternionf input) {
    return input.set(this.sinHalf, 0.0F, 0.0F, this.cosHalf);
  }
  
  public Quaternionf aroundY(Quaternionf input) {
    return input.set(0.0F, this.sinHalf, 0.0F, this.cosHalf);
  }
  
  public Quaternionf aroundZ(Quaternionf input) {
    return input.set(0.0F, 0.0F, this.sinHalf, this.cosHalf);
  }
  
  public float cos() {
    return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
  }
  
  public float sin() {
    return 2.0F * this.sinHalf * this.cosHalf;
  }
  
  public Matrix3f aroundX(Matrix3f input) {
    input.m01 = 0.0F;
    input.m02 = 0.0F;
    input.m10 = 0.0F;
    input.m20 = 0.0F;
    float c = cos();
    float s = sin();
    input.m11 = c;
    input.m22 = c;
    input.m12 = s;
    input.m21 = -s;
    input.m00 = 1.0F;
    return input;
  }
  
  public Matrix3f aroundY(Matrix3f input) {
    input.m01 = 0.0F;
    input.m10 = 0.0F;
    input.m12 = 0.0F;
    input.m21 = 0.0F;
    float c = cos();
    float s = sin();
    input.m00 = c;
    input.m22 = c;
    input.m02 = -s;
    input.m20 = s;
    input.m11 = 1.0F;
    return input;
  }
  
  public Matrix3f aroundZ(Matrix3f input) {
    input.m02 = 0.0F;
    input.m12 = 0.0F;
    input.m20 = 0.0F;
    input.m21 = 0.0F;
    float c = cos();
    float s = sin();
    input.m00 = c;
    input.m11 = c;
    input.m01 = s;
    input.m10 = -s;
    input.m22 = 1.0F;
    return input;
  }
}