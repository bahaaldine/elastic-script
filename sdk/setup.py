"""
Moltler SDK Setup
"""

from setuptools import setup, find_packages

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name="moltler",
    version="0.1.0",
    author="Moltler Team",
    author_email="moltler@example.com",
    description="Python SDK for the Moltler AI Skills Creation Framework",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/baha/elastic-script",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: System :: Systems Administration",
    ],
    python_requires=">=3.9",
    install_requires=[
        "requests>=2.25.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
            "black>=23.0.0",
            "mypy>=1.0.0",
            "responses>=0.23.0",
        ],
    },
    entry_points={
        "console_scripts": [
            # No CLI for SDK - use escript-cli instead
        ],
    },
)
