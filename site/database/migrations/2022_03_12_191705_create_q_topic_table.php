<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQTopicTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('q_topic', function (Blueprint $table) {
            $table->increments('Qt_id');
            $table->integer('Q_id')->comment('Quest ID')->unsigned()->nullable();
            $table->string('Qt_name', 350);
            $table->integer('Qt_order');
            $table->boolean('Qt_multi');
            $table->boolean('Qt_freetext');


            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Q_id')->references('Q_id')->on('q_quest');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('q_topic');
    }
}
