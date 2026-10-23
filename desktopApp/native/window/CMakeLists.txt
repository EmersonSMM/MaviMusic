cmake_minimum_required(VERSION 3.16)

project(mavimusic_window LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

foreach(suffix "" _DEBUG _RELEASE _RELWITHDEBINFO _MINSIZEREL)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY${suffix} ${CMAKE_BINARY_DIR})
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY${suffix} ${CMAKE_BINARY_DIR})
endforeach()

add_library(mavimusic_window SHARED window_jni.cpp)
set_target_properties(mavimusic_window PROPERTIES PREFIX "")

if(DEFINED ENV{JAVA_HOME})
    target_include_directories(mavimusic_window PRIVATE
        $ENV{JAVA_HOME}/include
        $ENV{JAVA_HOME}/include/win32
    )
else()
    find_package(JNI REQUIRED)
    target_include_directories(mavimusic_window PRIVATE ${JNI_INCLUDE_DIRS})
endif()

target_link_libraries(mavimusic_window PRIVATE user32 dwmapi ole32 uuid)

if(MINGW)
    # The desktop app loads this DLL from its private runtime directory. Gradle daemons and
    # installed builds do not inherit the developer toolchain's bin directory, so leaving MinGW's
    # libwinpthread dynamic makes the bridge silently unavailable even though the DLL itself was
    # packaged correctly.
    target_link_options(mavimusic_window PRIVATE -static -static-libgcc -static-libstdc++)
endif()
