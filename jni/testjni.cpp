#include <string.h>
#include <jni.h>
extern "C"{
      JNIEXPORT jstring JNICALL
      Java_my_opcv_OpencvLoader_stringFromJNI
      (JNIEnv *env, jobject obj)
      {
    	  return env->NewStringUTF("Test JNI - ok :)");
      }
}
