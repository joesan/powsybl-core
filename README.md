[![Build Status](https://travis-ci.org/powsybl/powsybl-core.svg?branch=master)](https://travis-ci.org/powsybl/powsybl-core)
[![Build status](https://ci.appveyor.com/api/projects/status/76o2bbmsewpbpr97/branch/master?svg=true)](https://ci.appveyor.com/project/powsybl/powsybl-core/branch/master)
[![Coverage Status](https://coveralls.io/repos/github/powsybl/powsybl-core/badge.svg?branch=master)](https://coveralls.io/github/powsybl/powsybl-core?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/59d241d16725bd4557abc716/badge.svg?style=flat)](https://www.versioneye.com/user/projects/59d241d16725bd4557abc716)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

# Powsybl - core
http://www.powsybl.com

## Environment requirements
In order to build the project, you need:
  * JDK *(1.8 or greater)*
  * Maven 
  * CMake *(2.6 or greater)*
  * Recent C++ compiler (GNU g++ or Clang)
  * OpenMPI *(1.8.3 or greater)*
  * Some development packages (zlib, bzip2)

### OpenMPI (required)
In order to support the MPI modules, you need to compile and install the [OpenMPI](https://www.open-mpi.org/) library.
```
$> wget http://www.open-mpi.org/software/ompi/v1.8/downloads/openmpi-1.8.3.tar.bz2
$> tar xjf openmpi-1.8.3.tar.bz2
$> cd openmpi-1.8.3
$> ./configure --prefix=<INSTALL_DIR> --enable-mpi-thread-multiple
$> make install
$> export PATH=$PATH:<INSTALL_DIR>/bin
```

### zlib (required)
In order to build Boost external package, you have to install [zlib](http://www.zlib.net/) library.
```
$> yum install zlib-devel
```

### bzip2 (required)
In order to build Boost external package, you have to install [bzip](http://www.bzip.org/) library.
```
$> yum install bzip2-devel
```

## Install
To easily compile, you can use the toolchain:
```
$> git clone https://github.com/powsybl/powsybl-core.git
$> ./install.sh
```
By default, the toolchain will:
  * download and compile all external packages from the Internet
  * compile C++ and Java modules
  * install the platform

### Targets

| Target | Description |
| ------ | ----------- |
| clean | Clean modules |
| clean-thirdparty | Clean the thirdparty libraries |
| compile | Compile modules |
| package | Compile modules and create a distributable package |
| __install__ | __Compile modules and install it__ |
| docs | Generate the documentation (Doxygen/Javadoc) |
| help | Display this help |

### Options

The toolchain options are saved in the *install.cfg* configuration file. This configuration file is loaded and updated
each time you use the toolchain.

#### Global options

| Option | Description | Default value |
| ------ | ----------- | ------------- |
| --help | Display this help | |
| --prefix | Set the installation directory | $HOME/itools |
| --package-type | Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 | zip |

#### Third-parties

| Option | Description | Default value |
| ------ | ----------- | ------------- |
| --with-thirdparty | Enable the compilation of thirdparty libraries | |
| --without-thirdparty | Disable the compilation of thirdparty libraries | |
| --thirdparty-prefix | Set the thirdparty installation directory | $HOME/powsybl_thirdparty |
| --thirdparty-download | Sets false to compile thirdparty libraries from a local repository | true |
| --thirdparty-packs | Sets the thirdparty libraries local repository | $HOME/powsybl_packs |

### Default configuration file
```
#  -- Global options --
powsybl_prefix=$HOME/itools
powsybl_package_type=zip

#  -- Thirdparty libraries --
thirdparty_build=true
thirdparty_prefix=$HOME/powsybl_thirdparty
thirdparty_download=true
thirdparty_packs=$HOME/powsybl_packs
```
