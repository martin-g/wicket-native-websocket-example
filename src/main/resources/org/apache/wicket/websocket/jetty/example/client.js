/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

jQuery(function($) {

	Wicket.Event.subscribe("/websocket/open", function(jqEvent) {
		$('#connexion').hide();
		$('#sentMessages').show();
	});

	Wicket.Event.subscribe("/websocket/message", function(jqEvent, message) {
		$('#messages').prepend('<span>' + message + '</span><br/>');
	});

	var close = function(jqEvent) {
		$('#sentMessages').hide();
		$('#connexion').show();
		$('#messages').empty();
	};

	Wicket.Event.subscribe("/websocket/closed", close);
	Wicket.Event.subscribe("/websocket/error", close);

	$('#connect').click(function() {
		Wicket.WebSocket.createDefaultConnection();
	});

	$('#send').click(function() {
		Wicket.WebSocket.send($('#message').val());
		$('#message').val('');
	});

	$('#disconnect').click(function() {
		Wicket.WebSocket.close();
	});

});
