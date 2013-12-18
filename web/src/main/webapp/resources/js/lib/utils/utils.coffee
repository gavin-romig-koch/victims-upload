define ['underscore', 'moment'], (_, moment) ->

  utils = {}

  utils.truthy = (obj) ->
    if obj is undefined
      return false
    else if _.isBoolean obj
      return obj
    else if _.isString obj
      return if _.contains ['YES', 'yes', 'Y', 'y', '1', 'true', 'TRUE', 'ok', 'OK'], obj then true else false
    else if _.isNumber obj
      return parseInt(obj) is 1
    else
      return false

  utils.isInteger = (f) -> f isnt undefined and typeof(f) is 'number' and Math.round(f) == f
  utils.isFloat = (f) -> f isnt undefined and typeof(f) is 'number' and !utils.isInteger(f)

  utils.stringify = (obj) ->
    if _.isString obj
      return obj
    else if _.isObject obj
      return JSON.stringify obj

    return obj

  utils.parseToJson = (data) ->
    if typeof data is "object"
      return data
    else if data is ""
      return undefined
    else if data is undefined
      return undefined
    else
      return JSON.parse(data)

  utils.parseInteger = (n) ->
    if _.isNumber n
      return n
    else if typeof n is "object"
      return undefined
    else if n is undefined
    else if n is ""
      return undefined
    else if n is undefined
      return undefined
    else
      return parseInt n

  utils.parseMetricName = (input) ->
    splitInput = input.split(':')
    if splitInput.length != 2
      console.error "d3Metric attribute metric-name requires a group:name input"
    else
      metric =
        group: splitInput[0]
        name: splitInput[1]

      return metric

  utils.isUnixOffset = (theInput) -> /[0-9]{13}/.test(theInput)
  utils.isUnixTimestamp = (theInput) -> /[0-9]{10}/.test(theInput) and String(theInput).length is 10

  # Parses a variety of inputs to a unix offset (ms)
  utils.parseDateToOffset = (theDate, opts = {}) ->
    format = opts?.format
    utc = opts?.utc || true

    # Assume if a number and if of length 1230768000000 then a unix offset, length of 10 is unix timestamp
    isUnixOffset = utils.isUnixOffset(theDate)
    isUnixTimestamp = utils.isUnixOffset(theDate)

    pFormat = switch format
      when 'year'  then 'YYYY'
      when 'month' then 'YYYY-MM'
      when 'day'   then 'YYYY-MM-DD'
      when 'hour'  then 'YYYY-MM-DD HH'
      when 'minute'  then 'YYYY-MM-DD HH:mm'
      when 'second'  then 'YYYY-MM-DD HH:mm:ss'
      else  undefined

    if isUnixOffset
      return if utc then +moment.utc(theDate) else +moment(theDate)
    else if isUnixTimestamp
      return +moment.unix(theDate)
    if _.isDate theDate
      return +moment.utc(theDate)
    else if format is 'year' and _.isNumber(theDate)
      return if utc then +moment.utc(String(theDate), pFormat) else +moment(String(theDate), pFormat)
      # The default here is theDate is a String
    else
      return if utc then +moment.utc(theDate, pFormat) else +moment(theDate, pFormat)

  return utils
