local log_file = nil
local MainPanel = GetDevice(0)


local HOST_PORT = 14801  -- Choose your desire port where DCS is running
local ANDROID_PORT = 14800 -- Choose your desire port for android device


local HEAD_MSG = "THRDCS"
local count=0
local msgOut =""
local clientIP=""
local speed =""
local thr = 30;
local engineNow = thr;

local prevLuaExportStart = LuaExportStart


function LuaExportStart()
	
	if prevLuaExportStart then
        prevLuaExportStart()
    end

	log_file = io.open(lfs.writedir().."/Logs/Export.log", "w")
	

	package.path  = package.path..";"..lfs.currentdir().."/LuaSocket/?.lua"
  	package.cpath = package.cpath..";"..lfs.currentdir().."/LuaSocket/?.dll"
 	
 	socket = require("socket")
 	c = socket.udp()
	c:setsockname("*", HOST_PORT)
	c:setoption('broadcast', true)
	c:settimeout(0)
end



function LuaExportBeforeNextFrame()

	data, ip, port = c:receivefrom()

	if data then
        --log_file:write("---------------------------------------------------\n")
        --log_file:write("Received: ", data)
        --log_file:write("\n")
        clientIP = ip
  
        local dataArray = string.gmatch(data, '([^,]+)')

        if dataArray(0)==HEAD_MSG then
        	thr = dataArray(1)
        	--log_file:write("thr: ", thr)
        	--log_file:write("\n")
        	gear = dataArray(2)
        	flaps = dataArray(3)
        	brakeOff = dataArray(4)
        	brakeOn = dataArray(5)

        	if brakeOn == "1" then
        		--log_file:write("brakeOn:  ")
				--log_file:write("\n")
        		LoSetCommand(577)
        	end 

        	if brakeOff == "1" then
        		--log_file:write("brakeOff:  ")
				--log_file:write("\n")
        		LoSetCommand(578)
        	end

        	if brakeOn == "0" and brakeOff == "0" then
        		LoSetCommand(579)
        	end

        	setGear(gear)

        	setFlaps(flaps)
        	
        end  
        checkBalance(thr,engineNow)      
    end
end



function LuaExportAfterNextFrame()

	local engine_thr_left_now = MainPanel:get_argument_value(8) * 100
	local engine_thr_right_now = MainPanel:get_argument_value(9) * 100

	engineNow = math.floor((engine_thr_left_now + engine_thr_right_now) / 2)
	--log_file:write("engines: ", engineNow)
	--log_file:write("\n")

	gearNow =  math.floor(MainPanel:get_argument_value(716))
	--log_file:write("gearNow: ", gearNow)
	--log_file:write("\n")

	flapsNow =  math.floor(MainPanel:get_argument_value(653)*100)
	--log_file:write("flapsNow: ", flapsNow)
	--log_file:write("\n")

	speed = math.floor(MainPanel:get_argument_value(48)*100) -- Currently not is use
	--log_file:write("Speed: ", speed)
	--log_file:write("\n")


	msgOut = HEAD_MSG..","..engineNow..","..gearNow..","..flapsNow.." \n"
	c:sendto(msgOut, clientIP, ANDROID_PORT)
	log_file:write("MSG: ", msgOut)
    --log_file:write("\n")

    --log_file:write("IP: ", clientIP..":",ANDROID_PORT)
    --log_file:write("\n")
	
end


function checkBalance(remote,current)


	remote = tonumber(remote)
	current = tonumber(current)
	
	if math.abs( remote - current) < 2 then
		LoSetCommand(1034) -- stop 
		return 0
	end

	if remote < current then
		LoSetCommand(1033) -- decrease thrust
	elseif remote > current then
		LoSetCommand(1032) -- increase thrust

		
	end

end

function setGear(GearDown)
	
	if GearDown == "1" then
		LoSetCommand(431)
	
	else
		LoSetCommand(430)
	end
end


function setFlaps(flps)

	
	if flps == "0" then
		LoSetCommand(1047)
	elseif flps == "10" then
		LoSetCommand(1048)
	elseif flps == "20" then
		LoSetCommand(1049)
	end

end




function LuaExportStop()
   if log_file then
   	log_file:write("Closing log file...")
   	log_file:close()
   	log_file = nil
   end

   --c:close
end

function LuaExportActivityNextEvent(t)
	local tNext = t

	tNext = tNext + 0.1

	return tNext
end

