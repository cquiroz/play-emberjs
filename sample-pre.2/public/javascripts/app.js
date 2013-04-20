App = Ember.Application.create();

App.ApplicationController = Ember.Controller.extend();

App.SubView = Ember.View.extend({
  templateName: "sub/sub"
});

App.Router = Ember.Router.extend({
  root: Ember.Route.extend({
    index: Ember.Route.extend({
      route: '/',
      connectOutlets: function(router) {
        router.get('applicationController').connectOutlet('sub')
      }
    })
  })
});

App.initialize();