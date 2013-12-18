define ["angular", "services", "jquery", "nv", "moment", 'd3Logic', 'utils'], (angular, services, $, nv, moment, d3Logic, utils) ->
  "use strict"
  angular.module("rhphd.directives", ["rhphd.services"])
  .directive("appVersion", ["version", (version) ->
    return (scope, elm, attrs) ->
      elm.text version
  ])
  .directive 'json', () ->
    return {
      require: 'ngModel',
      link: (scope, elm, attrs, ctrl) ->
        ctrl.$parsers.unshift (viewValue) ->
          # For not if no value or '' return true.  This should not return true if required but no docs on that I can find
          if viewValue is undefined or viewValue is ''
            ctrl.$setValidity('json', true)
            return viewValue

          # Otherwise attempt to parse the string as json return false if error
          try
            JSON.parse(viewValue)
            ctrl.$setValidity('json', true)
            return viewValue
          catch e
            ctrl.$setValidity('json', false)
            return undefined
          return true
    }
  .directive "d3Tree", ($timeout) ->
    return {
      restrict: "E",
      scope: {
        val: "="
      }
      link: (scope, element, attrs) ->

        elementId = element.attr('id')
        containerSelector = "##{elementId}"

        dataSet = undefined
        chart = undefined
        duration = 500

        createTree = () ->
          # The base64 are the grey-plus and grey-minus images
          chart = nv.models.indentedTree()
            .iconOpen("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9wBBBQgHxiJ6ugAAADXSURBVDjL1ZKhDsJADIb/Ed6BZ0ABAoVBIpFIECRI3J5hbh7DI2BxGBQKhT0HCYJ/rqgijttu3Bg4QpNL2qRf/7ZX4O8tcs75etPt/mgDKRL4Eg96LfQ77SiotN7sNDdRpXtUNVQ1F6ox1O5oqj7XCCrdgcz54jrIAAEMGaQXBSSEh5NZDhNABV/uIFAudVG9xGZpWb7y08aLpd1RmtYX8NU26xQQCzuQAlBYP4K/MD+V8v4Oyr/gw2LbroMrRzCkhcSp87tLPBxPOo+Tj6e7SuLqS/yZPQAbdn01WTa53QAAAABJRU5ErkJggg==")
            .iconClose("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9wBBBQeNPXwCNUAAAC0SURBVDjLY2AY8oARxnj++v3/nUcvQjg/EAo+oPGtDcUZTPU1GTFMWrDhwH84+PH//wcY/vD//4MP//8/ePHh/4MHH/4beCT8R9bHhGHSTwaGjzD2D5gLPiLYaABhwA8iNP/AZwAD6ZoZGBgYWFACC6rQISIRM4wmTGD48AOPAcg2b1gwAcPmDzhcgOIFXM7GpRkzFghp/kHQAPyaP+BLiacvXv+fWt5JMOnO7izHnhIHDAAALe92s7vgY+oAAAAASUVORK5CYII=")
            .tableClass('table table-striped') #for bootstrap styling
            .columns([
              {
                key: 'key',
                label: 'Entity',
                showCount: true,
                width: '75%',
                type: 'text',
                classes: (d) -> if d.url? then return 'clickable name' else return 'name'
                click: (d) -> if d.url? then window.location.href = d.url else undefined
              },
              {
                key: 'type',
                label: 'Type',
                width: '25%',
                type: 'text'
              }]);
          return

        updateTree = () ->
          d3.select(containerSelector)
          .datum(dataSet)
          .transition().duration(duration).call(chart)

          chart.update()
          return

        handleTree = () ->
          if chart is undefined
            nv.addGraph () ->
              createTree()

              d3.select(containerSelector)
              .datum(dataSet)
              .transition().duration(duration).call(chart)

              nv.utils.windowResize chart.update

              return chart
            # Otherwise just update the data and redraw
          else
            updateTree()

        scope.$watchCollection "val", (newVal) ->
          if newVal
            dataSet = newVal
            # Handle updating or creating the tree
            handleTree()

        # It pains me to implement this hack.  The core issue is FF doesn't dynamically inheirit the height from parent
        # Containers like Chrome does.  This causes the graph to have a very small height and not really visible.
        # Whenever the window is resized, change the height of the svg element
        $(window).resize () ->
          # If on firefox resize the svg div
          #if not $(window).chrome
          if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
            innerHeight = $(window).innerHeight()
            #innerHeightAdjusted = innerHeight - ($("#main-navigation").height() / 2)

            $(containerSelector).height(innerHeight)

        #$(window).trigger('resize')
        if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          $timeout ( ->
            $(window).trigger('resize')
          ), 300

    }
  .directive("d3Metric", ['$interpolate', "$timeout", "metricService", ($interpolate, $timeout, metricService) ->
      # TODO add attributes for begin and end date potentially and limits.
      return {
        restrict: "E",
        scope: {
          machine: "=",
          metric: "=",
          metricName: "@" # ex. metric-name="mem:free" to dynamically construct a metric
          type: "@",
          legend: "=",
          limit: "@" # Define a limit, ex. to use with showing only the latest record
          max: "@" # If max is true, project the max, if it exists, into another stream
        }
        link: (scope, element, attrs) ->

          # Define the element selectors -- TODO there should be a way around this entire concept
          elementId = undefined
          containerSelector = undefined
          svgSelector = undefined
          svgElement = element.children('svg')[0]

          machine = undefined
          metric = undefined
          metricName = undefined
          dataSet = undefined
          chartType = "discreteBarChart"
          chart = undefined
          chartXType = undefined
          showLegend = true
          max = false
          limit = undefined
          duration = 1000
          height = 400

          margin = {top: 10, right: 50, bottom: 50, left: 70}

          setAxisFormatting = (dataSet) ->
            chart.xAxis.tickFormat (d) -> moment(d).format('MM-DD HH:mm:ss')

          createChartByType = () ->
            if chartType is 'multiBarChart'
              chart = nv.models.multiBarChart()
              .margin(margin)
              .x((d) -> d.x)
              .y((d) -> d.y)
              .tooltip((key, x, y, e, graph) ->
                  return "<h3>#{key}</h3><p>#{y} on #{x}</p>"
                )
              .showLegend(showLegend)
            # TODO the discreteBarChart currently does not work
            if chartType is 'discreteBarChart'
              chart = nv.models.discreteBarChart()
              .margin(margin)
              .x((d) -> d.x)
              .y((d) -> d.y)
            else if chartType is 'stackedAreaChart'
              chart = nv.models.stackedAreaChart()
              .margin(margin)
              .x((d) -> d.x)
              .y((d) -> d.y)
              .clipEdge(true)
              .tooltip((key, x, y, e, graph) ->
                  return "<h3>#{key}</h3><p>#{y} on #{x}</p>"
                )
              .showLegend(showLegend)
            else if chartType is 'line'
              chart = nv.models.lineChart()
              .margin(margin)
              .x((d) -> d.x)
              .y((d) -> d.y)
              .showLegend(showLegend)
            else if chartType is 'lineWithFocusChart'
              chart = nv.models.lineWithFocusChart()
              .margin(margin)
              .x((d) -> d.x)
              .y((d) -> d.y)
              .showLegend(showLegend)
            return undefined

          updateChartByType = () ->
            if _.contains ['multiBarChart', 'discreteBarChart', 'stackedAreaChart', 'line', 'lineWithFocusChart'], chartType
              #console.debug "Updating the d3 graph with: #{JSON.stringify(dataSet)}"

              setAxisFormatting(dataSet)

              d3.select(svgElement)
              .datum(dataSet)
              .transition().duration(duration).call(chart)

              chart.update?()

            return undefined

          handleChart = () ->
            # If no chart create the chart and add it to nv
            if chart is undefined
              #console.log "Creating a new d3 Graph"
              nv.addGraph () ->
                createChartByType()
                setAxisFormatting(dataSet)

                #d3.select(svgSelector)
                d3.select(svgElement)
                .datum(dataSet)
                .transition().duration(duration).call(chart)

                nv.utils.windowResize chart.update

                return chart
              # Otherwise just update the data and redraw
            else
              updateChartByType()

          handlePie = () ->
            # If no chart create the chart and add it to nv
            if chart is undefined
              #console.debug "Creating a new Pie Graph with dataSet: #{JSON.stringify(dataSet)}"
              nv.addGraph () ->
                chart = nv.models.pieChart()
                .x((d) -> return d.key)
                .y((d) -> return d.y)
                .showLabels(true)
                #.showLegend(showLegend)

                d3.select(svgElement)
                .datum(dataSet)
                .transition().duration(duration).call(chart)

                chart.update()

                nv.utils.windowResize chart.update

                return chart
              # Otherwise just update the data and redraw
            else
              #console.debug "Updating the Pie graph with: #{JSON.stringify(dataSet)}"

              #d3.select(svgSelector)
              d3.select(svgElement)
                .datum(dataSet)
                .transition().duration(duration).call(chart)

              chart.update?()

          handleBubble = () ->
            chart = new Bubble "graph_container"
            chart.initialize_data dataSet
            chart.start()
            chart.display_group_all()

          # This method is called on changes to any attributes and handles pulling the metrics in, transforming to d3
          # creating/updating the chart
          handleOptionsChanges = () ->

            # If both machine and metric are defined, we have enough data to go ahead and create the graph
            # This logic will only be all true once all variables resolve to true, ie, the directive is fully parsed
            if machine? and (metric? or metricName?) and chartType?

              # Since at the time of linking the attribute id is not available, have to manually compute here
              #elementId = "#{metric.group}-#{metric.name}-#{machine._id}"
              #console.debug "elementId: #{elementId}"
              #containerSelector = "##{elementId}"
              #svgSelector = "##{elementId} svg"

              opts =
                machineId: machine._id
                group: metric.group
                name: metric.name

              if _.isNumber(utils.parseInteger(scope.limit))
                limit = parseInt(scope.limit)
                opts.limit = limit

              # Parse max here since it is an @
              max = utils.truthy scope.max

              metricService.getMetrics opts, (data) ->

                opts =
                  machine: machine
                  metric: metric
                  data: data
                  limit: limit
                  max: max
                  chartType: chartType

                # d3'ify the metric data
                d3Logic.transformMetricData opts, (d3Data) ->
                  dataSet = d3Data
                  #console.debug "Transformed metrics to d3 streams: #{JSON.stringify(dataSet)}"

                  if dataSet?.length > 0
                    if chartType in ['multiBarChart', 'discreteBarChart', 'stackedAreaChart', 'line', 'lineWithFocusChart']
                      handleChart()
                    else if chartType is 'bubble'
                      handleBubble()
                    else if chartType is 'pie'
                      handlePie()
                    else
                      console.warn "Data to graph but no type of Graph selected!"
                  else
                    console.warn "No data given to graph!"

          resetSvg = () ->
            chart = undefined
            console.debug "Resetting SVG"
            #d3.selectAll(svgSelector).remove()
            #d3.select(containerSelector).append("svg")

            $(element).children().remove()
            $(element).append("svg")

          # This probably will have no effect for our uses in the phd
          scope.$watch "type", (newVal, oldVal) ->
            if newVal
              chartType = newVal

              #console.debug "chartType changed to: #{chartType}"

              # If the type of the graph has changed, remove the current element
              if (newVal isnt oldVal and newVal isnt undefined) then resetSvg()

              # Now handle the options changes
              handleOptionsChanges()

            # Trigger a resize if firefox so the svg can be sized accordingly
            #if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
            #  $timeout ( ->
            #    $(window).trigger('resize')
            #  ), 300

          scope.$watch "metric", (newVal, oldVal) ->
            if newVal

              metric = utils.parseToJson newVal

              # Optionally set the chartType from the metric if it exists
              if metric.type? then chartType = metric.type

              #console.debug "Metric changed to: #{JSON.stringify(metric)}"

              handleOptionsChanges()

          scope.$watch "metricName", (newVal, oldVal) ->
            if newVal
              metric = utils.parseMetricName newVal

              #console.debug "Metric changed to: #{JSON.stringify(metric)} via metricName: #{metricName}"

              handleOptionsChanges()

          scope.$watch "machine", (newVal, oldVal) ->
            #Make sure the newVal is defined
            if newVal and (newVal._id isnt oldVal?._id)

              machine = newVal

              #console.debug "Machine changed to: #{JSON.stringify(machine)}"

              handleOptionsChanges()

          #scope.$watch "legend", (newVal, oldVal) ->
          #  showLegend = newVal
          #  console.debug "Legend changed to: #{newVal}"
          #  # If the type of the graph has changed, remove the current element
          #  if (newVal isnt oldVal and newVal isnt undefined) then resetSvg()
          #
          #  # Now handle the options changes
          #  handleOptionsChanges()
          #
          #  # Trigger a resize if firefox so the svg can be sized accordingly
          #  if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          #    $timeout ( ->
          #      $(window).trigger('resize')
          #    ), 300


          # It pains me to implement this hack.  The core issue is FF doesn't dynamically inheirit the height from parent
          # Containers like Chrome does.  This causes the graph to have a very small height and not really visible.
          # Whenever the window is resized, change the height of the svg element
          #$(window).resize () ->
          #  # If on firefox resize the svg div
          #  if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          #    innerHeight = $(window).innerHeight()
          #    #innerHeightAdjusted = innerHeight - ($("#main-navigation").height() / 2)
          #
          #    # Previously working
          #    #$("#graph_container svg").height(innerHeight)
          #    $(svgSelector).height(innerHeight)
          #    $(svgElement).height(innerHeight)


          #$(window).trigger('resize')
          #if navigator.userAgent.toLowerCase().indexOf('firefox') > -1
          #  $timeout ( ->
          #    $(window).trigger('resize')
          #  ), 300
        };
  ])
