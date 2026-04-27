cmake_minimum_required(VERSION 3.22.1)
project("lab02")

add_library(
        detector
        SHARED
        root_detector.cpp
        frida_detector.cpp
)

find_library(log-lib log)
target_link_libraries(detector ${log-lib})

target_compile_options(detector PRIVATE -std=c++17 -O2)