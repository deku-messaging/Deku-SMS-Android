#!/usr/bin/env python3

import sys

if sys.argv[1] == 'staging':
    print('beta')
elif sys.argv[1] == 'master' or sys.argv[1] == 'main':
    print('production')
else:
    print('internal')
