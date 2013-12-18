define ['underscore'], (_) ->

  uriPrepend = if not /.*?(foo\.redhat\.com|localhost|itos).*?/.test window?.location?.origin then "/labs/redhat-console" else ""

  d3Logic = {}


  # Takes a list of metrics and transforms to d3 streams.  The raw form of the data is:
  #[
  #{
  #  "_id": "52a64345cc061d97d7093148",
  #  "uuid": "cf32b06d-4744-41d9-9f4e-ecd40a7f8c65",
  #  "group": "mem",
  #  "name": "free",
  #  "value": 489698878,
  #  "date": 1386627849109
  #},
  #{
  #  "_id": "52a64345cc061d97d7093149",
  #  "uuid": "cf32b06d-4744-41d9-9f4e-ecd40a7f8c65",
  #  "group": "mem",
  #  "name": "free",
  #  "value": -1703480214,
  #  "date": 1386627789109
  #},
  #...
  #]
  d3Logic.transformMetricData = (opts, callback) ->
    d3Stream = []
    stream = {
      # TODO this should be data driven from mongo, something like #{metric.display}
      key: "#{opts.metric.group}:#{opts.metric.name}"
      values: []
    }

    if (opts.chartType is 'pie') and (opts.max is true) and (opts.limit is 1) and (opts.data.length is 1)
      # Get the first value, since the limit is known to be 1
      value = opts.data[0]

      # Reconstruct the stream
      # See: https://github.com/novus/nvd3/blob/master/examples/pieChart.html  for how to format data
      d3Stream = [
        {
          key: opts.metric.name
          y: value.value
        },
        {
          key: "total"
          y: value.max
        }
      ]
    else
      _.each opts.data, (d) ->
        stream.values.push
          x: d.date
          y: d.value
          max: d.max

      d3Stream.push stream

    callback d3Stream


  d3Logic.groupAccountByOrg = (accountData, callback) ->
    # Get the unique org ids as a has for reference
    uniqueOrgs = {}
    _.each(accountData, (m) -> _.each(m.orgs, (o) -> if (not _.has uniqueOrgs, o.orgId) then uniqueOrgs[o.orgId] = o))

    groupedData = {}

    # Now iterate over each piece of data
    _.each accountData, (m) ->
      _.each m['orgs'], (o) ->
        if not _.has groupedData, o.orgId
          groupedData[o.orgId] = []

        groupedData[o.orgId].push m

    callback groupedData

  d3Logic.accountToTree = (opts, callback) ->
    accountTree = [] # Temp holder so as not to trigger the directive too soon

    groupBy = opts.groupBy || "account"

    # Get the unique org ids as a has for reference
    orgsMap = {}
    _.each(opts.data, (m) -> _.each(m.orgs, (o) -> if (not _.has orgsMap, o.orgId) then orgsMap[o.orgId] = o))

    # Top level account which will contain an array of machines
    accountStream =
      key: opts.accountId
      type: 'Account'
      url: "#{uriPrepend}/#/accounts/#{opts.accountId}"
      values: []

    # If regularly grouping by account push each individual machine onto the org stream
    # Otherwise if grouping by machine, we want all machines directly under the account
    if groupBy is "account"

      # Group by the orgId.  The groupedData will contain the orgId: [machines]
      # One problem here is the key is just the orgId, we need more info though.
      d3Logic.groupAccountByOrg opts.data, (groupedData) ->

        # For own is like for value,key in map
        _.forOwn groupedData, (machines, orgId) ->

          orgStream =
          # orgName if it exists otherwise orgId
            key: orgsMap[orgId]['orgName'] || orgId
            type: 'Organization'
            url: "#{uriPrepend}/#/accounts/#{accountStream.key}/orgs/#{orgId}"
            values: []

          _.each machines, (machine) ->
            # TODO hostname if it exists otherwise machineId.  Assuming hostname could be bad as there may be many
            # machines with localhost
            machineStream =
              key: machine['hostname'] || machine['_id']
              type: 'Machine'
              url: "#{uriPrepend}/#/accounts/#{accountStream.key}/orgs/#{orgId}/machines/#{machine['_id']}"
              values: []

            # Push each individual organization onto the account stream
            orgStream.values.push machineStream

          # After each unique org stream is pushed with machines, now push the org stream
          accountStream.values.push orgStream

    else if groupBy is "machine"
      _.each opts.data, (machine) ->
        # TODO hostname if it exists otherwise machineId.  Assuming hostname could be bad as there may be many
        # machines with localhost
        machineStream =
          key: machine['hostname'] || machine['_id']
          type: 'Machine'
          url: "#{uriPrepend}/#/accounts/#{accountStream.key}/orgs/#{machine.orgId}/machines/#{machine['_id']}"
          values: []

        # Push each individual organization onto the account stream
        accountStream.values.push machineStream

    # Push the account stream onto the main tree object
    accountTree.push accountStream

    # Finally return the accountTree
    callback accountTree

  return d3Logic