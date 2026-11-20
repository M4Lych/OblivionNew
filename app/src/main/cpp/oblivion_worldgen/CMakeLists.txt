cmake_minimum_required(VERSION 3.22)
project(oblivion_worldgen LANGUAGES CXX)

set(OWG_SOURCES
    src/overworld.cpp
    src/nether_end.cpp
    src/config.cpp
    src/jni.cpp)

if(ANDROID)
    find_package(shadowhook REQUIRED CONFIG)
    list(APPEND OWG_SOURCES src/probe.cpp)
endif()

add_library(oblivion_worldgen SHARED ${OWG_SOURCES})

target_include_directories(oblivion_worldgen PUBLIC include PRIVATE src)
target_compile_features(oblivion_worldgen PRIVATE cxx_std_17)
target_compile_options(oblivion_worldgen PRIVATE -O2 -fvisibility=hidden)
target_link_options(oblivion_worldgen PRIVATE -Wl,-z,max-page-size=16384)

if(ANDROID)
    target_link_libraries(oblivion_worldgen PRIVATE shadowhook::shadowhook log)
endif()
