from pathlib import Path
import setuptools

__version__ = "0.2.3"

info = Path(__file__).with_name("README.md").read_text(encoding="utf8")

setuptools.setup(
    name="repro-apk",
    url="https://github.com/obfusk/reproducible-apk-tools",
    description="scripts to make android apks reproducible",
    long_description=info,
    long_description_content_type="text/markdown",
    version=__version__,
    author="FC Stegerman",
    author_email="flx@obfusk.net",
    license="GPLv3+",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Environment :: Console",
        "Intended Audience :: Developers",
        "Intended Audience :: Information Technology",
        "Intended Audience :: System Administrators",
        "Intended Audience :: Telecommunications Industry",
        "License :: OSI Approved :: GNU General Public License v3 or later (GPLv3+)",
        "Operating System :: MacOS :: MacOS X",
        "Operating System :: POSIX :: Linux",
        "Operating System :: POSIX",
        "Operating System :: Unix",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
      # "Programming Language :: Python :: 3.12",
        "Programming Language :: Python :: Implementation :: CPython",
        "Programming Language :: Python :: Implementation :: PyPy",
        "Topic :: Software Development",
        "Topic :: Utilities",
    ],
    keywords="android apk reproducible",
    entry_points=dict(
        console_scripts=[
            "repro-apk = repro_apk:main",
            "repro-apk-inplace-fix = repro_apk.inplace_fix:main",
        ]
    ),
    packages=["repro_apk"],
    package_data=dict(repro_apk=["py.typed"]),
    python_requires=">=3.8",
    install_requires=["click>=6.0"],
)
