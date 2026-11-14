cmake_minimum_required(VERSION 3.22.1)
project(oblivion_hosted_runtime C)

add_library(oblivion_minecraftpe SHARED oblivion_minecraftpe_forwarder.c)

target_compile_options(oblivion_minecraftpe PRIVATE
    -Wall -Wextra -Wformat=2 -fvisibility=hidden
)
target_link_options(oblivion_minecraftpe PRIVATE -Wl,-z,max-page-size=16384)
target_link_libraries(oblivion_minecraftpe android log dl)
