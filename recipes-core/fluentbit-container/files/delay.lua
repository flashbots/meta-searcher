-- A multi-record delay script for Fluent Bit.

-- Store logs in a global buffer until they're old enough
buffer = {}

local DELAY_SEC = 10

function log_delay(tag, ts_table, record)
    -- Current time in integer seconds
    local now_sec = os.time()

    -- Capture the log arrival time (from Fluent Bit's time_as_table)
    local arrival_sec = ts_table.sec or 0

    -- Add to our buffer
    table.insert(buffer, {
        arrival_time = arrival_sec,
        record       = record
    })

    -- Build a list of logs to flush (>= DELAY_SEC old),
    -- and a list of logs that still need to wait.
    local to_emit = {}
    local still_waiting = {}

    for _, item in ipairs(buffer) do
        local age = now_sec - item.arrival_time
        if age >= DELAY_SEC then
            table.insert(to_emit, item.record)
        else
            table.insert(still_waiting, item)
        end
    end

    -- Keep the waiting logs in the buffer
    buffer = still_waiting

    -- If no logs are old enough, simply produce *no* records.
    -- lua code=2 => "use the same timestamp, replace the record with an array".
    if #to_emit == 0 then
        return 2, ts_table, {}
    end

    -- If we have logs old enough to flush, we return them *all* at once.
    -- lua code=1 => "replace timestamp + record"
    -- We'll set the new timestamp to 'now' for all flushed logs.
    local new_ts = { sec = now_sec, nsec = 0 }
    return 1, new_ts, to_emit
    
end