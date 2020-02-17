[![Maven Central](https://img.shields.io/maven-central/v/tech.harmonysoft/leonardo)](https://img.shields.io/maven-central/v/tech.harmonysoft/leonardo)

## Tl;DR

A chart drawing kotlin Android library designed to work with dynamic data loading and unloading.

## Rationale

Originally this library was created [Telegram chart contest](https://t.me/contest/7). However, when the contest was done, it looked like it was worth to made it production-ready and open-source. The rationale is simple - there is a number of great chart libraries, but they work with static data, i.e. it's developer's responsibility to pre-load target data points, feed them into a library and build a more or less static chart.

With this library we have a dedicated [ChartView](library/src/main/kotlin/tech/harmonysoft/oss/leonardo/view/chart/ChartView.kt) with highly customizable [ChartConfig](library/src/main/kotlin/tech/harmonysoft/oss/leonardo/model/config/chart/ChartConfigBuilder.kt) and powerful [ChartModel](library/src/main/kotlin/tech/harmonysoft/oss/leonardo/model/runtime/ChartModel.kt). The model should be provided by [ChartDataLoader](library/src/main/kotlin/tech/harmonysoft/oss/leonardo/model/data/ChartDataLoader.kt) by the application (data loading strategy interface). It's already guaranteed to be called from a background thread. The library also takes care of applying loaded data points in the main thread.

## Example

A sample application which illustrates the library in action can be found on [Play Market](TBD). It uses infinite on-the-fly data loading strategy.