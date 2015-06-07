require 'artoo'

connection :sphero, :adaptor => :sphero, :port => '/dev/tty.Sphero-BOR-RN-SPP'
device :sphero, :driver => :sphero

work do
  @count = 1
  @angle = 0
  every(3.seconds) do
  	sphero.set_color(@count % 2 == 0 ? :green : :blue)
  	#sphero.stop
  	sphero.roll 50, @angle
  	@count += 1
  	@angle += 90
  	#sphero.stop
  end
end
