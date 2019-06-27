# Copyright 2019, The Android Open Source Project
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
Cache Finder class.
"""

import atest_utils
from test_finders import test_finder_base

class CacheFinder(test_finder_base.TestFinderBase):
    """Cache Finder class."""
    NAME = 'CACHE'

    def __init__(self, **kwargs):
        super(CacheFinder, self).__init__()

    def find_test_by_cache(self, test_reference):
        """Find the matched test_infos in saved caches.

        Args:
            test_reference: A string of the path to the test's file or dir.

        Returns:
            A list of TestInfo namedtuple if cache found, else None.
        """
        return atest_utils.load_test_info_cache(test_reference)
