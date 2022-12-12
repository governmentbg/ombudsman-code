//
// Dynamsoft JavaScript Library for Basic Initiation of Dynamic Web TWAIN
// More info on DWT: http://www.dynamsoft.com/Products/WebTWAIN_Overview.aspx
//
// Copyright 2017, Dynamsoft Corporation 
// Author: Dynamsoft Team
// Version: 13.1
//
/// <reference path="dynamsoft.webtwain.initiate.js" />
var Dynamsoft = Dynamsoft || { WebTwainEnv: {} };

Dynamsoft.WebTwainEnv.AutoLoad = false;

///
Dynamsoft.WebTwainEnv.Containers = [];

/// If you need to use multiple keys on the same server, you can combine keys and write like this 
/// Dynamsoft.WebTwainEnv.ProductKey = 'key1;key2;key3';
Dynamsoft.WebTwainEnv.ProductKey = 'f0068NQAAAI0rr9FpVjxHD3w/RABdyfMYry3Gg/OfQIN02n56pwh50HRtb1b9w4mzt7JD11x5z7aLIsYU03NATa0w9G2Hpdw=';

///
Dynamsoft.WebTwainEnv.Trial = false;

///
//Dynamsoft.WebTwainEnv.ActiveXInstallWithCAB = false;

Dynamsoft.WebTwainEnv.ScanDirectly = false; //za da ne se pokazvat izobrajeniyata vuv view-ara
///
Dynamsoft.WebTwainEnv.ResourcesPath = '../resources/ResourcesDS';

/// All callbacks are defined in the dynamsoft.webtwain.install.js file, you can customize them.
// Dynamsoft.WebTwainEnv.RegisterEvent('OnWebTwainReady', function(){
// 		// webtwain has been inited
// });

