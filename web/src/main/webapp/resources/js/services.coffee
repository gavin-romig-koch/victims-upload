#https://access.redhat.com/services/user/status
define ["angular", "jquery", "underscore", "toastr", 'd3Logic'], (angular, $, _, toastr, d3Logic) ->
  "use strict"
  angular.module("rhphd.services", []).value("version", "0.1")
    # Comes with the angular seed
    .service("sharedProperties", ['notificationService', 'accountService', 'machineService', (notify, accountService, machineService) ->
      # Create a container to hold all shared properties within the app
      service = {}

      # Variables common to many controllers
      service.accountId = undefined
      service.accountData = undefined
      service.accountTree = undefined
      service.orgId = undefined
      service.machineId = undefined
      service.machine = undefined
      # Track at what level in the heirarchy is being vieweed.  Ie. account|org|machine
      service.uiSelection = undefined

      # Contains the set of dashboard metrics we want to show by default
      # TODO eventually this should be read from the database
      service.defaultDashboardMetrics = [
        {group: 'mem', name: 'free', type: "line"},
        {group: 'mem', name: 'total', type: "line"}
      ]

      # TODO if accountId is the same as the routeParam don't reset accountData or Tree otherwise do reset
      # Handle the route parms.  Let's do it here in one place instead of duplicating this in each controller
      service.handleRouteParams = (routeParams) ->
        # If the accountId route param exists, and it should
        if routeParams["accountId"]?

          # If the accountId is already defined and equals the routeParams one, and there is data, don't reload that data
          if not ((service.accountId is routeParams['accountId']) and service.accountData?)
            service.accountId = routeParams['accountId']
            service.uiSelection = "account"
            accountService.getAccount {accountId: service.accountId}, (data) ->
              service.accountData = data
              opts =
                accountId: service.accountId
                data: service.accountData
                groupBy: "account"

              d3Logic.accountToTree opts, (result) ->
                service.accountTree = result
        else
          notify.error "Unable to load page without an accound id"

        # Handle when the orgId is set
        if routeParams['orgId']
          service.uiSelection = "org"
          service.orgId = routeParams['orgId']

        # Handle when the machine uuid is set.
        if routeParams['machineId']
          service.uiSelection = "machine"
          service.machineId = routeParams['machineId']

        # When both the machineId, orgId, and accountId are present, then we can fetch the machine
        if service.machineId
          opts =
            machineId: service.machineId
          machineService.getMachine opts, (data) ->
            service.machine = data
            #console.debug "Loaded machine: #{JSON.stringify(service.machine)}"
      return service
    ])
    # Abstracted Notification service, can easily switch out the impl this way
    .factory('notificationService', ['$http', ($http) ->
      service = {}
      service.error = (data) -> toastr.error data
      service.success = (data) -> toastr.success data
      return service
    ])
    # Account service to interface with the API accounts REST API
    .factory('accountService', ['$http', '$location', '$cookies', 'notificationService', ($http, $location, $cookies, notify) ->
      # By default on dev/itos the /api should be /api
      # If we are on the chromified /labs env, all ajax calls must be prepended
      ajaxPrepend = if not /.*?(foo\.redhat\.com|localhost|itos).*?/.test $location.$$host then "/labs/redhat-console" else ""
      config =
        withCredentials: true

      # Generic on error that provides default error formatting and notifications
      onError = (data, status, headers, config) ->
        theHtml = "<p>There was a #{status} accessing #{config.url}</p>";
        if _.has(data, 'ERROR')
          theHtml += "<br /><p><b>ERROR:</b> #{data.ERROR} </p>"
          notify.error theHtml

      service = {}
      service.getAccount = (opts, callback) ->

        $http.get("#{ajaxPrepend}/api/accounts/#{opts.accountId}", config)
        .success((data) -> callback(data))
        .error(onError)

      # Returns all accounts
      service.getAccounts = (callback) ->
        $http.get("#{ajaxPrepend}/api/accounts", config)
          .success((data) -> callback(data))
          .error(onError)

#      service.saveAccount = (accountId, callback) ->
#        if not accountId? then notify.error "Please enter an account id"; return;
#        $http.post("#{ajaxPrepend}/api/accounts", {"accountId": accountId, "machines": []})
#          .success((data) -> callback(data))
#          .error(onError)
#
#      service.deleteAccount = (accountId, callback) ->
#        if not accountId? then notify.error "Please enter an account id"; return;
#        $http.delete("#{ajaxPrepend}/api/accounts/#{accountId}")
#        .success((data) -> callback(data))
#        .error(onError)

      service.createTestData = (accountId, callback) ->
        if not accountId? then notify.error "Please enter an account id"; return;
        $http.post("#{ajaxPrepend}/api/admin/accounts/#{accountId}", config)
        .success((data) -> callback(data))
        .error(onError)

      service.deleteTestData = (accountId, callback) ->
        if not accountId? then notify.error "Please enter an account id"; return;
        $http.delete("#{ajaxPrepend}/api/admin/accounts/#{accountId}", config)
        .success((data) -> callback(data))
        .error(onError)

      return service
    ])
    # Account service to interface with the API accounts REST API
    .factory('machineService', ['$http', '$location', '$cookies', 'notificationService', ($http, $location, $cookies, notify) ->
      # By default on dev/itos the /api should be /api
      # If we are on the chromified /labs env, all ajax calls must be prepended
      ajaxPrepend = if not /.*?(foo\.redhat\.com|localhost|itos).*?/.test $location.$$host then "/labs/redhat-console" else ""
      config =
        withCredentials: true

      # Generic on error that provides default error formatting and notifications
      onError = (data, status, headers, config) ->
        theHtml = "<p>There was a #{status} accessing #{config.url}</p>";
        if _.has(data, 'ERROR')
          theHtml += "<br /><p><b>ERROR:</b> #{data.ERROR} </p>"
          notify.error theHtml

      service = {}

      service.getMachine = (opts, callback) ->
        #if not opts.accountId? then notify.error "Please enter an account id"; return;
        #if not opts.orgId? then notify.error "Please enter a orgId"; return;
        if not opts.machineId? then notify.error "Please enter a machine uuid"; return;
        $http.get("#{ajaxPrepend}/api/machines/#{opts.machineId}", config)
        #$http.get("#{ajaxPrepend}/api/accounts/#{opts.accountId}/orgs/#{opts.orgId}/machines/#{opts.machineId}", {})
        .success((data) -> callback(data))
        .error(onError)
        return

      service.getMachines = (opts, callback) ->
        if not opts.accountId? then notify.error "Please enter an account id"; return;
        $http.get("#{ajaxPrepend}/api/accounts/#{opts.accountId}/machines", config)
        .success((data) -> callback(data))
        .error(onError)
        return

      service.saveMachine = (opts, callback) ->
        if not opts.machineId? then notify.error "Please enter a machine id"; return;
        if not opts.accountId? then notify.error "Please enter an account id"; return;
        $http.post("#{ajaxPrepend}/api/accounts/#{opts.accountId}/machines/#{opts.machineId}", config)
        .success((data) -> callback(data))
        .error(onError)
        return

      service.deleteMachine = (opts, callback) ->
        if not opts.accountId? then notify.error "Please enter an account id"; return;
        if not opts.machineId? then notify.error "Please enter a machine id"; return;
        $http.delete("#{ajaxPrepend}/api/accounts/#{opts.accountId}/machines/#{opts.machineId}", config)
        .success((data) -> callback(data))
        .error(onError)
        return

      return service
    ])
    # Account service to interface with the API accounts REST API
    .factory('metricService', ['$http', '$location', '$cookies', 'notificationService', ($http, $location, $cookies, notify) ->
      # By default on dev/itos the /api should be /api
      # If we are on the chromified /labs env, all ajax calls must be prepended
      ajaxPrepend = if not /.*?(foo\.redhat\.com|localhost|itos).*?/.test $location.$$host then "/labs/redhat-console" else ""

      # Generic on error that provides default error formatting and notifications
      onError = (data, status, headers, config) ->
        theHtml = "<p>There was a #{status} accessing #{config.url}</p>";
        if _.has(data, 'ERROR')
          theHtml += "<br /><p><b>ERROR:</b> #{data.ERROR} </p>"
          notify.error theHtml

      service = {}

      service.getMetrics = (opts, callback) ->
        if not opts.machineId? then notify.error "Please enter a machine uuid"; return;
        if not opts.group? then notify.error "Please enter a metric group"; return;
        if not opts.name? then notify.error "Please enter a metric name"; return;

        config = {}
        config.withCredentials = true
        if opts.beginDate then params.beginDate = opts.beginDate
        if opts.endDate then params.endDate = opts.endDate
        if opts.limit isnt undefined
          config.params = {}
          config.params.limit = opts.limit

        url = "#{ajaxPrepend}/api/machines/#{opts.machineId}/metrics/#{opts.group}/#{opts.name}"
        $http.get(url, config)
          .success((data) -> callback(data))
          .error(onError)
        return

      return service
    ])
