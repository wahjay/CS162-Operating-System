require "sinatra"
require 'yaml'
require "erb"

# YAML file, for my understanding, also for the purpose of this exercise,
# is used to store a simple hash.

# web url
# http://localhost:4567/members?


# to use HTTP session
enable :sessions
# to fake HTTP method verb (like delete or put)
use Rack::MethodOverride

# used to store members info
filename = "info.yml"


# store the submitted data into a file
def store_info(filename, name, age, gender, job)
  @members_info = YAML.load_file(filename)
  @members_info = {} unless @members_info

  @info = {"age" => age, "gender" => gender, "job" => job}
  @members_info[name] = @info
  File.write(filename, @members_info.to_yaml)
end

# read all the submitted data from the file and return it
def read_names(filename)
  return [] unless File.exist?(filename)
  @members_info = YAML.load_file(filename)
  @members_info.keys
end

# remove a given name
def remove_name(filename, name)
  return nil unless File.exist?(filename)
  @members_info = YAML.load_file(filename)
  return nil if @members_info.empty? or @members_info.nil?
  @members_info.delete(name)
  File.write(filename, @members_info.to_yaml)
end

# validate user input
class NameValidator
  def initialize(name, age, gender, job, names)
    @name = name.to_s
    @age = age.to_s
    @gender = gender.to_s
    @job = job.to_s
    @names = names
    @message = nil
  end

  # safe to update?
  def safe?
    update
    @message.nil?
  end

  # valid to create?
  def valid?
    validate
    @message.nil?
  end

  def age
    @age.strip
  end

  def name
    @name.strip
  end

  def gender
    @gender.strip
  end

  def job
    @job.strip
  end

  def message
    @message
  end

  private

   def validate
     if name.empty? or age.empty? or gender.empty? or job.empty?
       @message = "Nothing should be left blank."

     else
       @names.each do |n|
         if n.casecmp(name) == 0
           n = n.split.map(&:capitalize).join(" ")
           @message = "#{n} is already included in our list."
         end
       end
     end
   end

   def update
     if name.empty? or age.empty? or gender.empty? or job.empty?
       @message = "Nothing should be left blank."
     end
   end
end

# Display all members
get "/members" do
  @names = read_names(filename)
  erb :index
end

# Display a form for a new member
get "/members/new" do
  erb :new
end

# Display a single member
get "/members/:name" do
  @message = session.delete(:message)
  @name = params["name"]
  @age = params["age"]
  @gender = params["gender"]
  @job = params["job"]

  # if directly click a member name from index page
  # to check that member's info, we need to extract his info
  # from a hash file
  unless @age or @gender or @job
    @members_info = YAML.load_file(filename)
    @info = @members_info[@name]
    @info = {} unless @info
    @age = @info["age"]
    @gender = @info["gender"]
    @job = @info["job"]
  end

  erb :show
end


# Create that new member
post "/members" do
  @name = params["name"]
  @age = params["age"]
  @gender = params["gender"]
  @job = params["job"]

  validator = NameValidator.new(@name, @age, @gender, @job, read_names(filename))

  if validator.valid?
    store_info(filename, @name, @age, @gender, @job)
    session[:message] = "Successfully stored the information for #{@name}."
    # redirect to a new page with the given query parameters
    redirect "/members/#{@name.gsub(" ","%20")}?age=#{@age}&gender=#{@gender}&job=#{@job}"
  else
    # re-render the form again due to the invalid input
    @message = validator.message
    erb :new
  end
end


# Display a form for editing a member
get "/members/:name/edit" do
  @name = params[:name]
  @members_info = YAML.load_file(filename)
  @info = @members_info[@name]

  if @info
    @age = @info["age"]
    @gender = @info["gender"]
    @job = @info["job"]
  end
  erb :edit
end

# Update that new member
put "/members/:name" do
  @old_name = params["name"]
  @age = params["age"]
  @gender = params["gender"]
  @job = params["job"]
  @new_name = params["new_name"]

  validator = NameValidator.new(@new_name, @age, @gender, @job, read_names(filename))

  if validator.safe?
    @members_info = YAML.load_file(filename)
    @info = {"age" => @age, "gender" => @gender, "job" => @job}
    @members_info[@old_name] = @info

    # here to update info and remains position if required
    @members_info[@new_name] = @members_info.delete(@old_name)
    File.write(filename, @members_info.to_yaml)

    session[:message] = "Successfully updated the infomation for #{@new_name}."
    redirect "/members/#{@new_name.gsub(" ","%20")}?age=#{@age}&gender=#{@gender}&job=#{@job}"
  else
    @message = validator.message
    erb :edit
  end
end

# Ask for a confirmation to delete
get "/members/:name/delete" do
  @name = params["name"]
  @message = "Are you sure you want to remove member "
  erb :delete
end

# Delete a member
delete "/members/:name" do
  remove_name(filename, params["name"])
  redirect "/members"
end
