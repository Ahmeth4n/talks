cmake_minimum_required(VERSION 3.22.1)
project("lab03")

add_library(
        detector
        SHARED
        root_detector.cpp
        frida_detector.cpp
        crc_guard.cpp
)

find_library(log-lib log)

target_link_libraries(
        detector
        ${log-lib}
        dl
)

target_compile_options(detector PRIVATE -std=c++17 -O2)