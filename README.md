# 投资再平衡计算器

Android Kotlin / Jetpack Compose 计算工具：录入持仓金额和目标比例，查看当前占比、目标金额、偏离与调整金额，并计算按目标分配投入和新增资金再平衡。

当前源码 **0.1.0（code 1）**，截至 2026-10-06 核对；持续交接见 [HANDOFF.md](HANDOFF.md)。

## 下载与构建

[v0.1.0 测试 APK](https://github.com/matt-xb/investment-rebalance-tool/releases/tag/v0.1.0) 是预发布调试包。

需要 JDK 17、Android SDK 35，最低 Android 8.0（API 26）。Android Studio 打开仓库后配置本机 SDK，再执行：

```powershell
.\gradlew.bat assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`。

## 计算口径与当前边界

目标比例应合计 100%。新增资金再平衡采用源码中的离散买入规则：每次增加 200 元买入金额，每个参与买入的资产计 5 元费用，依次选择能改善偏离且不超预算的方案；不保证全局最优。普通按目标分配投入与新增资金再平衡是不同计算入口。

口径见 [RebalanceCalculator.kt](app/src/main/java/com/example/rebalance/domain/RebalanceCalculator.kt)。APK 元数据核对不能替代计算验收；本轮仅更新说明，下一步用已知结果样例验证预算、费用、目标合计及边界输入，再核对真机持久化与操作。

仓库尚未选定开源许可证，公开可见不等于已授予任意再分发许可。
