# Google Spreadsheets input plugin for Embulk [![Build Status](https://travis-ci.org/kataring/embulk-input-google_spreadsheets.svg?branch=master)](https://travis-ci.org/kataring/embulk-input-google_spreadsheets) [![Gem Version](https://badge.fury.io/rb/embulk-input-google_spreadsheets.svg)](http://badge.fury.io/rb/embulk-input-google_spreadsheets)

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: yes
* **Guess supported**: no

## Usage

### Install plugin

```
embulk gem install embulk-input-google_spreadsheets
```

## Configuration

- **service_account_email**: Your Google service account email (string, required)
- **p12_keyfile**: Fullpath of private key in P12(PKCS12) format (string, required)
- **spreadsheet_id**: Your spreadsheet id (string, required)
- **sheet_index**: sheet index (int, optional default: 0)
- **application_name**: Anything you like (string, optional defaulf: "Embulk-GoogleSpreadsheets-InputPlugin")
- **columns**: 

## Example

```yaml
in:
  type: google_spreadsheets
  service_account_email: 'XXXXXXXXXXXXXXXXXXXXXXXX@developer.gserviceaccount.com'
  p12_keyfile: '/tmp/embulk.p12'
  spreadsheet_id: '1RPXaB85DXM7sGlpFYIcpoD2GWFpktgh0jBHlF4m1a0A'
  columns:
    - {name: id, type: long}
    - {name: account, type: long}
    - {name: time, type: timestamp, format: '%Y-%m-%d %H:%M:%S'}
    - {name: purchase, type: timestamp, format: '%Y%m%d'}
    - {name: comment, type: string}
```


## Build

```
$ ./gradlew gem 
```
