cmake_minimum_required(VERSION 3.18.1)
project("virtualhook")

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_definitions(-DANDROID)
add_definitions(-D__ANDROID_API__=28)

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

file(GLOB native_sources 
    "*.c"
    "*.cpp"
)

add_library(${PROJECT_NAME} SHARED ${native_sources})

target_link_libraries(${PROJECT_NAME}
    android
    log
)
