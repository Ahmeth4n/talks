cmake_minimum_required(VERSION 3.22.1)
project("lab01")

add_library(rootdetector SHARED root_detector.cpp)

find_library(log-lib log)
target_link_libraries(rootdetector ${log-lib})

target_compile_options(rootdetector PRIVATE -std=c++17 -O2)