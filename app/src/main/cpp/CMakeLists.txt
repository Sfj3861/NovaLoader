cmake_minimum_required(VERSION 3.22.1)
project(nova_runtime_probe LANGUAGES CXX)

add_library(nova_runtime_probe SHARED native_runtime_probe.cpp)

target_compile_features(nova_runtime_probe PRIVATE cxx_std_17)
target_compile_options(nova_runtime_probe PRIVATE -Wall -Wextra -Werror)
