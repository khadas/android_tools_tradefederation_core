# Copyright 2017, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Various globals used by atest.
"""

MODE = 'DEFAULT'

# Result server constants for atest_utils.
RESULT_SERVER = ''
RESULT_SERVER_ARGS = []
RESULT_SERVER_TIMEOUT = 5

# Arg constants.
WAIT_FOR_DEBUGGER = 'WAIT_FOR_DEBUGGER'
DISABLE_INSTALL = 'DISABLE_INSTALL'
PRE_PATCH_ITERATIONS = 'PRE_PATCH_ITERATIONS'
POST_PATCH_ITERATIONS = 'POST_PATCH_ITERATIONS'
PRE_PATCH_FOLDER = 'PRE_PATCH_FOLDER'
POST_PATCH_FOLDER = 'POST_PATCH_FOLDER'

# Application exit codes.
EXIT_CODE_SUCCESS = 0
EXIT_CODE_ENV_NOT_SETUP = 1
EXIT_CODE_BUILD_FAILURE = 2
EXIT_CODE_ERROR = 3
EXIT_CODE_TEST_NOT_FOUND = 4