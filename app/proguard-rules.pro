# ML Kit crea sus "ComponentRegistrar" por reflexión; sin esto R8 borra los constructores
# y el OCR falla en release con un NullPointerException.
-keep class * implements com.google.firebase.components.ComponentRegistrar { public <init>(); }
