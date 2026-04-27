cmake_minimum_required(VERSION 3.22.1)
project("detector")

add_library(
        detector
        SHARED


        crc_guard.cpp
        root_detector.cpp
        frida_detector.cpp


        native_detection_thread.cpp
)

find_library(log-lib log)

target_link_libraries(
        detector
        ${log-lib}
)