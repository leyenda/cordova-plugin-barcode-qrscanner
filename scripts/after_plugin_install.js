#!/usr/bin/env node

/**
 * Cordova hook to build JavaScript assets after plugin installation.
 * This ensures www/www.min.js and other built files are available.
 */

'use strict';

var fs = require('fs');
var path = require('path');
var exec = require('child_process').exec;

module.exports = function(context) {
    var pluginDir = context.opts.plugin.dir;
    var wwwFile = path.join(pluginDir, 'www', 'www.min.js');

    // Check if build is needed (www.min.js doesn't exist)
    if (fs.existsSync(wwwFile)) {
        console.log('cordova-plugin-barcode-qrscanner: Built files already exist, skipping build.');
        return;
    }

    console.log('cordova-plugin-barcode-qrscanner: Building JavaScript assets...');

    return new Promise(function(resolve, reject) {
        // Run npm install and build in the plugin directory
        var command = 'cd "' + pluginDir + '" && npm install --production=false && npm run build';

        exec(command, { maxBuffer: 1024 * 1024 * 10 }, function(error, stdout, stderr) {
            if (error) {
                console.error('cordova-plugin-barcode-qrscanner: Build failed!');
                console.error(stderr);
                // Don't reject - try to continue anyway in case pre-built files exist elsewhere
                console.warn('cordova-plugin-barcode-qrscanner: Installation may fail if www/www.min.js is missing.');
                resolve();
            } else {
                console.log('cordova-plugin-barcode-qrscanner: Build completed successfully.');
                resolve();
            }
        });
    });
};
