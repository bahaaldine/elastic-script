"""
elastic-script CLI - A beautiful command-line interface for elastic-script.
"""

from setuptools import setup, find_packages

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name="escript-cli",
    version="0.1.0",
    author="Bahaaldine Azarmi",
    author_email="baha@elastic.co",
    description="A beautiful CLI for elastic-script - procedural scripting for Elasticsearch",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/bahaaldine/elastic-script",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Environment :: Console",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Topic :: Database",
        "Topic :: Software Development :: Interpreters",
    ],
    python_requires=">=3.9",
    install_requires=[
        "prompt_toolkit>=3.0.0",
        "rich>=13.0.0",
        "pygments>=2.15.0",
        "requests>=2.28.0",
        "click>=8.0.0",
        "toml>=0.10.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
            "black>=23.0.0",
            "mypy>=1.0.0",
        ],
    },
    entry_points={
        "console_scripts": [
            "escript=escript_cli.main:main",
        ],
        "pygments.lexers": [
            "escript=escript_cli.lexer:ElasticScriptLexer",
        ],
    },
    include_package_data=True,
)
