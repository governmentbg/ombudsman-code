<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQAnswersTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('q_answers', function (Blueprint $table) {
            $table->increments('Qa_id');
            $table->integer('Qt_id')->comment('Topic ID')->unsigned()->nullable();
            $table->string('Qa_name', 350);
            $table->integer('Qa_order');
            $table->boolean('Qa_freetext');


            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Qt_id')->references('Qt_id')->on('q_topic');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('q_answers');
    }
}
