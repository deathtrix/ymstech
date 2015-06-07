#!/usr/bin/env ruby

require "opencv"
require "json"
require_relative "astar"
include OpenCV
require 'artoo'

connection :sphero, :adaptor => :sphero, :port => '/dev/tty.Sphero-BOR-RN-SPP'
device :sphero, :driver => :sphero

# Load image
cvmat = OpenCV::CvMat.load("input.jpg")

# convert de mana
# mat = OpenCV::CvMat.new(cvmat.rows, cvmat.cols, :cv8u, 1)
# (cvmat.rows * cvmat.cols).times do |i|
#   mat[i] = (cvmat[i][0] <= 128) ? OpenCV::CvScalar.new(0) : OpenCV::CvScalar.new(255)
# end


puts "Building maze...\n"
out = OpenCV::CvMat.new(cvmat.rows, cvmat.cols, :cv8u, 1)
(cvmat.rows * cvmat.cols).times do |i|
  out[i] = OpenCV::CvScalar.new(0)
end

gray = cvmat.BGR2GRAY
gray = gray.smooth(CV_MEDIAN, 11)
bin = gray.threshold(128, 255, :binary)

#canny = bin.canny(128, 255)

contour = bin.find_contours(:mode => OpenCV::CV_RETR_EXTERNAL, :method => OpenCV::CV_CHAIN_APPROX_SIMPLE)
cindex=1
min_area = 1000000
while contour
  if contour && !contour.hole? && contour.contour_area > 20000 then
  	#puts "Contour ##{cindex} is #{contour.contour_area} px^2 (width: #{contour.bounding_rect.width}, height: #{contour.bounding_rect.height}, type: #{(contour.hole?)?"hole":"contour"})"
  	#puts "#{contour.bounding_rect.x}"
    poly = contour.approx(:accuracy => 1)
    cvmat = cvmat.draw_contours(poly, CvColor::Blue, CvColor::Red, 0, :thickness => 5, :line_type => :aa)
    out = out.draw_contours(poly, CvColor::White, CvColor::White, 0, :thickness => -1, :line_type => :aa)
    if contour.contour_area < min_area then
      min_area = contour.contour_area
    end
  end

  cindex += 1
  contour = contour.h_next
end 

puts "Resizing maze and calculating distances...\n"
postit_length = Math.sqrt(min_area).round
size = CvSize.new((out.cols/postit_length).round, (out.rows/postit_length).round);
out = out.resize(size)
out.save_image("maze-scaled.jpg")
cvmat.save_image("output-inter.jpg")
exit

puts "Calculating matrix...\n"
matrix = Array.new(out.rows * out.cols)
(out.rows * out.cols).times do |i|
  pixel = out[i][0]
  matrix[i] = (pixel.to_int == 255) ? 1 : 0
end

puts "Finding route...\n"
start       = { 'x' => 5, 'y' => 15 }
destination = { 'x' => 11, 'y' => 6 }
pathfinder  = Astar.new(start, destination, matrix, out.cols, out.rows)
result      = pathfinder.search

if (result.size > 0)
  i = 0
  steps = Array.new(result.size)
  result.each {|node|
    out[node.x()+node.y()*(100/postit_length).round] = CvColor::White
    steps[i] = Array.new(2)
    steps[i][0] = node.x()
    steps[i][1] = node.y()
    i += 1
  }
end

puts "Start walking...\n"
puts steps.to_json
work do
	rolling = false
	degree = 0
	speed = 70
	while not rolling
		rolling = sphero.roll 0, 0
	end

	sphero.set_color(:yellow)
	sphero.start_calibration
	sleep 7
	sphero.finish_calibration

	(1..steps.size-1).each{|i|
		dx = steps[i][0] - steps[i-1][0]
		#dy = steps[i][1] - steps[i-1][1]
		dy = steps[i-1][1] - steps[i][1]
		degree = Math.atan2(dx, dy) * 180 / Math::PI
		if degree < 0 then
			degree += 360
		end
		sphero.roll speed, degree
		sleep 0.25
	}
end

cvmat.save_image("output.jpg")
out.save_image("maze.jpg")

