"use strict";

/* See: https://stackoverflow.com/questions/30008114/how-do-i-promisify-native-xhr#30008115
 * 
 * opts = {
 *   method: String,
 *   url: String,
 *   data: String | Object,
 *   headers: Object,
 *   responseType: String,
 *   binary: Boolean,
 * }
 */
function request( opts) {
  return new Promise( function( resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.open( opts.method || 'GET', opts.url);
    if (opts.responseType) xhr.responseType = opts.responseType;
    xhr.onload = function () {
      if (this.status >= 200 && this.status < 300) {
        resolve(xhr);
      } else {
        reject( this);
      }
    };
    xhr.onerror = function () {
      reject( this);
    };
    if (opts.headers) {
      Object.keys(opts.headers).forEach(function (key) {
        xhr.setRequestHeader(key, opts.headers[key]);
      });
    }
    var params = opts.data;
    // We'll need to stringify if we've been given an object
    // If we have a string, this is skipped.
    if (params && typeof params === 'object' && !opts.binary) {
      params = Object.keys(params).map(function (key) {
        return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
      }).join('&');
    }
    xhr.send(params);
  });
}


var app = new Vue({
    
    // root <div> in page
    el: "#app",
    
    data: {

    	// Authenticated user
    	user: "Unknown user",

    	// todo list
    	todos: [],
    	
    	// todo in editor
    	todo: {},
    	
    	// extra task in editor
    	newtask: {},

        // alerts
        error: {},              // status alert
    },
    
    created: function() { // Runs on page load
        request( { url: 'todos/whoami' }).then( function( xhr) {
            app.user = JSON.parse( xhr.response);
            app.reload();
        });
    },
    
    methods: {
        
    	reload: function() {
            this.todo = {};
            request( { url: 'todos'}).then( function( xhr) {
                app.todos = JSON.parse( xhr.response);
            });
    	},
    	
    	edit: function( t) {
    		this.todo = {
    				id: t.id || undefined,
    				name: t.name || "",
    				description: t.description || "",
    				tasks: t.tasks || []
    		};
    		if (this.count( this.todo.tasks) == this.todo.tasks.length) {
    			this.todo.tasks.push( { name: "", description: ""});
    		}
    		this.$refs.editor.show();
    	},
    	
        save: function() {
    		request({
                url:  this.todo.id ? 'todos/' + this.todo.id : 'todos',
                method: this.todo.id ? 'PUT' : 'POST',
                data: JSON.stringify( this.todo),
                headers: {
                    'Content-Type': 'application/json; charset=utf-8',
                }
            }).then( function( xhr) {
                app.showInfo( "Saved");
                app.reload();
            }).catch( function( xhr) {
                app.showError( xhr.response);
            });
        },
        
        remove: function() {
    		this.$refs.editor.hide();
    		request({
                url:  'todos/' + this.todo.id,
                method: 'DELETE',
            }).then( function( xhr) {
                app.showInfo( "Deleted");
                app.reload();
            }).catch( function( xhr) {
                app.showError( xhr.response);
            });
        },
        
        count: function(tasks) {
        	let n = 0;
        	for (let i = 0; i < tasks.length; ++i) {
        		if (tasks[i].name && tasks[i].description) ++n;
        	}
        	return n;
        },

        showInfo: function( msg) {
            this.error = { counter: 5, type: 'success', msg: '' + msg }
        },
        
        // Flash a warning popup
        showWarning: function( msg) {
            this.error = { counter: 10, type: 'warning', msg: '⚠️ ' + msg }
        },
        
        // Report an error
        showError: function( msg) {
            this.error = { counter: 60, type: 'danger', msg: '⛔ ' + msg }
        },
    },
})
