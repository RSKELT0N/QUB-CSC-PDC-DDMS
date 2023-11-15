# CMake version specified
cmake_minimum_required(VERSION 3.9)

# Project declaration of base project.
project(Distributed-Data-Management-System
        VERSION 0.1
        DESCRIPTION "A Distributed data management system."
        LANGUAGES C CXX)

# Only perform if this is the root project
if(CMAKE_SOURCE_DIR STREQUAL CMAKE_CURRENT_SOURCE_DIR)

  set(PROJECT ddms)

  set(CMAKE_CXX_STANDARD 20)                          # Define the C++ standard
  message("-- C++ standard is set to -std=c++${CMAKE_CXX_STANDARD}")

  set(EXECUTABLE_NAME ddms)			              # Define executable name for project

# add submodules

# source
  add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/src)

  message("-- ${PROJECT_NAME} [${PROJECT_VERSION}] build files has been defined")
else()
  message("-- ${CMAKE_CURRENT_SOURCE_DIR}/CMakeLists.txt must be the root config and not added as a subdirectory")
endif()
