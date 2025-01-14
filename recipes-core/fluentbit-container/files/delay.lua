--
-- delay_all.lua
--
-- A multi-record delay script for Fluent Bit (time_as_table on).
-- - Temporarily using a 30s delay for testing (change back to 120s later).
-- - Stores logs in 'buffer' until they are >=30s old.
-- - On each new log, it flushes *all* logs older than 30s at once.
-- - Each flushed record gets a new timestamp (the time we actually output it),
--   plus two fields in the record: "orig_ts_sec" and "output_ts_sec" for tracking.
--

--------------------------------------------------------------------------------
-- Global buffer to hold logs until they're old enough, plus our current delay.
--------------------------------------------------------------------------------
buffer = {}

-- Delay in seconds (TEMPORARY: set back to 120s when done testing).
local DELAY_SEC = 30

--------------------------------------------------------------------------------
-- Function: log_delay(tag, ts_table, record)
-- Invoked for each new log that arrives. We add the new log to 'buffer', then
-- flush any logs older than DELAY_SEC in one shot (splitting the record).
--------------------------------------------------------------------------------
function log_delay(tag, ts_table, record)
    ----------------------------------------------------------------------------
    -- (1) Capture current time, store the new record in 'buffer'
    ----------------------------------------------------------------------------
    local now_sec = os.time()       -- integer: current system time in seconds
    local arrival_sec = ts_table.sec or 0

    -- Add a field so we know the original arrival time in the final record
    record["orig_ts_sec"] = arrival_sec

    -- Insert into our global buffer
    table.insert(buffer, {
        arrival_time = arrival_sec,
        record       = record
    })

    -- Debug print: show buffer size & the exact record we just inserted
    print(string.format("[stage 1] Inserted new log. Buffer size=%d. Record inserted=%s", 
        #buffer, tostring(record))
    )

    ----------------------------------------------------------------------------
    -- (2) Find all buffered records older than DELAY_SEC
    --
    -- We'll split the buffer into two lists:
    --   'to_emit': logs that are older than the threshold and ready to be flushed.
    --   'still_waiting': logs not yet old enough, remain in the buffer.
    ----------------------------------------------------------------------------
    local to_emit = {}
    local still_waiting = {}

    for _, item in ipairs(buffer) do
        local age = now_sec - item.arrival_time
        if age >= DELAY_SEC then
            table.insert(to_emit, item)
        else
            table.insert(still_waiting, item)
        end
    end

    -- Now replace our 'buffer' with just the logs that need more time
    buffer = still_waiting

    ----------------------------------------------------------------------------
    -- If there's nothing to emit, we "drop" the *incoming* record from output,
    -- but it still remains stored in 'buffer' for future flush.
    ----------------------------------------------------------------------------
    if #to_emit == 0 then
        print("[stage 2] No logs old enough to flush. Dropping current log from the pipeline.")
        -- code=2 => "keep timestamp, but replace record with an empty array",
        -- meaning zero new records pass along the pipeline.
        return 2, ts_table, {}
    end

    ----------------------------------------------------------------------------
    -- (3) We have logs that are old enough to emit.
    --
    --   - We'll build an array of "flushed" records. Each record gets a
    --     'output_ts_sec' field set to 'now_sec', so we know exactly
    --     when it was finally emitted.
    --   - We then return code=1, telling Fluent Bit that we're replacing
    --     both the timestamp and the record. 
    --   - We'll pass an array of records, so each item becomes a separate
    --     output event with the same new timestamp.
    ----------------------------------------------------------------------------
    local flushed_records = {}
    for _, item in ipairs(to_emit) do
        local old_rec = item.record
        local age = now_sec - item.arrival_time

        -- We record the moment of output
        old_rec["output_ts_sec"] = now_sec

        -- Debug print: show how old this record was, plus the original arrival time
        print(string.format(
            "[stage 3] Flushing delayed log (age=%ds) orig_ts_sec=%d, log='%s'",
            age, old_rec["orig_ts_sec"], tostring(old_rec["log"] or "(no log)")
        ))

        -- Collect it for splitting
        table.insert(flushed_records, old_rec)
    end

    -- All flushed records get the same 'new' timestamp in Fluent Bit
    local new_ts = { sec = now_sec, nsec = 0 }

    print(string.format("[stage 3] Emitting %d logs; new buffer size=%d", 
        #to_emit, #buffer))

    -- code=1 => replace the timestamp & record with these new values.
    -- The third return argument is an array, so these records are "split" 
    -- into multiple events.
    return 1, new_ts, flushed_records
end