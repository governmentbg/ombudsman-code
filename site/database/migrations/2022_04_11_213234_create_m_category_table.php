<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMCategoryTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_category', function (Blueprint $table) {
            $table->increments('Cat_id');
            $table->integer('Cat_parent_id')->comment('Parent ID')->unsigned()->nullable();
            $table->string('Cat_name', 255);


            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Cat_parent_id')->references('Cat_id')->on('m_category');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_category');
    }
}
