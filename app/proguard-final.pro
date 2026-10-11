# ===== 终章规则：必须在所有 consumer 规则之后执行 =====
# 强制移除 RuntimeVisibleAnnotations（剥离 Kotlin @Metadata，消除 .kt 文件名泄漏）
# navigation-common 的 consumer 规则会加回 RuntimeVisibleAnnotations，这里最后再删掉
-keepattributes !RuntimeVisibleAnnotations,!RuntimeVisibleParameterAnnotations
